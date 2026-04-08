# Welcome to Cloud Functions for Firebase for Python!
# To get started, simply uncomment the below code or create your own.
# Deploy with `firebase deploy`

from firebase_functions import https_fn
from firebase_functions.params import SecretParam
from openai import OpenAI
import requests
from bs4 import BeautifulSoup
import firebase_admin
from firebase_admin import initialize_app
import json

# Img inclusion
import asyncio
import base64
from concurrent.futures import ThreadPoolExecutor
import time
import uuid
import yt_dlp
import cv2
from transformers import CLIPProcessor, CLIPModel
from PIL import Image
import sys
import io

clip_model = None
clip_processor = None

initialize_app()

executor = ThreadPoolExecutor()

OPENAI_API_KEY = SecretParam("OPENAI_API_KEY")
SUPADATA_API_KEY = SecretParam("SUPADATA_API_KEY")
NEWS_API_KEY = SecretParam("NEWS_API_KEY")
HF_TOKEN = SecretParam("HF_TOKEN")

SYSTEM_PROMPT = "You are a fact and bias checking assistant for articles and transcripts."


# main function
# /////////////
# verify authentification, then for video:
#   download video and generate transcript grab relevant frames
#   from vid, upload transcript and frames to openai and wait
#   for response
# for article:
#   grab html, filter out unnecessary tags, and upload to openai
@https_fn.on_call(
    secrets=[OPENAI_API_KEY, SUPADATA_API_KEY, NEWS_API_KEY, HF_TOKEN],
    memory=4096
)
def analyze_url(req: https_fn.CallableRequest):
    # Require authentication (matches your earlier security intent)
    print("Auth object:", req.auth)
    if req.auth is None:
        raise https_fn.HttpsError(
            code=https_fn.FunctionsErrorCode.UNAUTHENTICATED,
            message="User must be signed in."
        )

    start_call = time.perf_counter()

    data = req.data or {}
    url = str(data.get("url", "")).strip()
    is_video = bool(data.get("isVideo", False))

    if not url:
        raise https_fn.HttpsError(
            code=https_fn.FunctionsErrorCode.INVALID_ARGUMENT,
            message="url is required."
        )

    try:
        if is_video:

            start_video_pipeline = time.perf_counter()

            async def run_video_pipeline():
                loop = asyncio.get_event_loop()

                video_task = None
                video_task = loop.run_in_executor(executor, download_video, url)

                transcript_task = asyncio.create_task(fetch_transcript_async(url))

                if video_task:
                    video_path = await video_task
                else:
                    video_path = None

                if video_path and not isinstance(video_path, str):
                    raise Exception(f"Invalid video_path type: {type(video_path)}")

                if video_path:
                    frames_task = asyncio.create_task(process_frames_async(video_path))
                else:
                    frames_task = asyncio.create_task(asyncio.sleep(0, result=[]))

                transcript = await transcript_task
                frames = await frames_task

                end_video_pipeline = time.perf_counter()
                print(f"[PERFORMANCE] full vid pipeline: {end_video_pipeline - start_video_pipeline:.2f} sec")

                return transcript, frames

            try:
                transcript, encoded_frames = asyncio.run(run_video_pipeline())
                text = transcript
            except Exception as e:
                print("[VIDEO PIPELINE ERROR]:", repr(e))
                encoded_frames = []
                text = "There seems to be an issue with analyzing this link. Please try again or use a different link."

        else:
            r = requests.get(url, timeout=30, headers={"User-Agent": "Mozilla/5.0"})
            if r.status_code >= 400 or not r.text:
                raise Exception(f"Article fetch HTTP {r.status_code}")

            text = extract_text_from_html(r.text)
            encoded_frames = []

        print("Encoded frames:", len(encoded_frames))
        if not text:
            raise Exception("No usable text extracted.")

    except Exception as e:
        raise https_fn.HttpsError(
            code=https_fn.FunctionsErrorCode.INTERNAL,
            message=f"Failed to retrieve content: {e}"
        )

    text = text[:12000]

    # OpenAI
    start_openai = time.perf_counter()

    try:
        client = OpenAI(api_key=OPENAI_API_KEY.value)
        user_prompt = build_user_prompt(text)

        content = [{"type": "input_text", "text": user_prompt}]

        # frames currently empty (stubbed)
        for img in encoded_frames:
            if not img:
                continue
            content.append({
                "type": "input_image",
                "image_url": f"data:image/jpeg;base64,{img}"
            })

        resp = client.responses.create(
            model="gpt-4.1-mini",
            temperature=0,
            input=[
                {"role": "system", "content": [{"type": "input_text", "text": SYSTEM_PROMPT}]},
                {"role": "user", "content": content},
            ],
        )

        model_output = resp.output_text.strip()

        try:
            analysis = json.loads(model_output)
        except Exception:
            raise https_fn.HttpsError(
                code=https_fn.FunctionsErrorCode.INTERNAL,
                message=f"Model returned invalid JSON: {model_output[:50]}"
            )

        end_openai = time.perf_counter()
        end_call = end_openai

        print(f"[PERFORMANCE] openai call: {end_openai - start_openai:.2f} sec")
        print(f"[PERFORMANCE] total call: {end_call - start_call:.2f} sec")

        print(f"\n\n[OUTPUT] text: {text}")
        issues = analysis.get("issues", [])
        image_issues = analysis.get("image_issues", [])
        bias_score = analysis.get("bias_score", 0)
        alignment = analysis.get("alignment", "Center")
        print(f"\n\n[OUTPUT] issues: {issues}")
        print(f"\n\n[OUTPUT] vid issues: {image_issues}")
        print(f"\n\n[OUTPUT] bias score: {bias_score}")
        print(f"\n\n[OUTPUT] alignment: {alignment}")


        return {
            "text": text,
            "issues": issues,
            "image_issues": image_issues,
            "bias_score": bias_score,
            "alignment": alignment
        }

    except https_fn.HttpsError as e:
        raise e  # dumb

    except Exception as e:
        print("[OPENAI ERROR RAW]:", repr(e))
        raise https_fn.HttpsError(
            code=https_fn.FunctionsErrorCode.INTERNAL,
            message=f"OpenAI request failed: {repr(e)}"
        )


def build_user_prompt(text: str) -> str:
    return f"""
You are performing a factual and political bias review of a transcript and/or images from the related video.

STRICT RULES:
- Only report issues if there is clear evidence of:
  (1) verifiably false factual claims
  (2) meaningful political bias
- Do NOT speculate.
- Do NOT force findings.
- Only identify issues that directly relate to the transcript's message.

TEXT ANALYSIS:
- When reporting issues, return CHARACTER POSITIONS within the transcript.
- The "start" value must be the index of the first character of the problematic text.
- The "end" value must be the index immediately after the final character.
- Indices are based on the EXACT transcript provided below.


IMAGE ANALYSIS:
If an issue is based on an image (not directly tied to a specific transcript span):
- Add it to "image_issues"
- Include the frame_index (0-based index from provided images)
- Do NOT assign start/end indices for image issues

Return VALID JSON ONLY.

JSON FORMAT:

{{
  "text": "the full text of the article/transcript",
  "issues": "An array of "[
    {{
      "id": "ISSUE_",
      "type": "left | right | fake",
      "start": number,
      "end": number,
      "explanation": "brief explanation of the issue tied to the transcript text"
    }}
  ],
  "image_issues": [
    {{
      "id": "IMG_1",
      "frame_index": number,
      "type": "left | right | fake | misleading",
      "explanation": "brief explanation of the issue found in the image"
    }}
  ],
  "bias_score": "number 1 through 10 indicating how biased or incorrect the article is",
  "alignment": "Left | Lean Left | Center | Lean Right | Right"
}}

If no issues:

{{
  "issues": [],
  "image_issues" : [],
  "bias_score": 0,
  "alignment": "Center"
}}

Transcript:
{text}
""".strip()


# using bs to filter out unrelated tags from our html response
def extract_text_from_html(html: str) -> str:
    soup = BeautifulSoup(html, "html.parser")

    for tag in soup(["script", "style", "nav", "footer", "header", "iframe", "form", "input",
                     "button", "canvas", "svg", "video", "audio", "link", "meta", "noscript"]):
        tag.decompose()

    article = soup.find("article")
    if article:
        text = article.get_text(" ", strip=True)
    else:
        paragraphs = soup.find_all("p")
        text = " ".join(p.get_text(" ", strip=True) for p in paragraphs)

    return text.strip()


# grabbing transcript for video, scrapping unnecessary content
async def fetch_transcript_async(url):
    supadata_endpoint = "https://api.supadata.ai/v1/transcript"

    def blocking_request():
        return requests.get(
            supadata_endpoint,
            params={"url": url},
            headers={"x-api-key": SUPADATA_API_KEY.value},
            timeout=60,
        )

    loop = asyncio.get_event_loop()
    r = await loop.run_in_executor(executor, blocking_request)

    # Checking why supadata is rate limiting
    if r.status_code == 429:
        retry_after = r.headers.get("Retry-After")
        print("[SUPADATA RATE LIMIT] Retry-After:", retry_after)
        raise Exception(f"Supadata HTTP 429 (Retry-After={retry_after})")


    if r.status_code >= 400:
        raise Exception(f"Supadata HTTP {r.status_code}")

    segments = r.json().get("content", [])
    text = ""

    for seg in segments:
        text += " " + seg.get("text", "").strip()

    return text.strip()


# Grab first frames and encode them,
# ideally we use relevant frames instead
# of just the first 5
def process_frames(video_path):
#     return []  # TEMP disable
    cap = cv2.VideoCapture(video_path)
    total_frames = int(cap.get(cv2.CAP_PROP_FRAME_COUNT))

    if total_frames <= 0:
        cap.release()
        return []

    # sample ~15 candidate frames from middle
    start = int(total_frames * 0.2)
    end = int(total_frames * 0.8)

    sample_count = 15
    indices = [
        int(start + (end - start) * i / (sample_count - 1))
        for i in range(sample_count)
    ]

    scored_frames = []

    for idx in indices:
        cap.set(cv2.CAP_PROP_POS_FRAMES, idx)
        ret, frame = cap.read()

        if not ret:
            continue

        score = score_frame(frame)
        scored_frames.append((score, frame))

    cap.release()

    # sorting by relevance
    scored_frames.sort(key=lambda x: x[0], reverse=True)

    # taking top 3, can adjust
    top_frames = [frame for _, frame in scored_frames[:3]]

    return [encode_frame(f) for f in top_frames if f is not None]


# video -> usable frames (stub)
def extract_frames(video_path):
    return []


# Taking our video from url, downloading the video,
# and saving it
def download_video(url):
    output_template = f"/tmp/{uuid.uuid4()}.%(ext)s"

    ydl_opts = {
        "format": "worst[ext=mp4]/mp4",
        "outtmpl": output_template,
        "quiet": True,
        "noplaylist": True,
        "nocheckcertificate": True,
        "ignoreerrors": False,
        "cachedir": False,
        "no_warnings": True,
        "restrictfilenames": True,
        "encoding": "utf-8",
    }

    try:
        original_stdout = sys.stdout
        original_stderr = sys.stderr
        sys.stdout = io.StringIO()
        sys.stderr = io.StringIO()

        with yt_dlp.YoutubeDL(ydl_opts) as ydl:
            result = ydl.extract_info(url, download=True)

        sys.stdout = original_stdout
        sys.stderr = original_stderr

        if not result:
            raise Exception("yt_dlp returned no result")

        file_path = ydl.prepare_filename(result)

        if isinstance(file_path, bytes):
            file_path = file_path.decode("utf-8", errors="ignore")

        return file_path

    except Exception as e:
        sys.stdout = original_stdout
        sys.stderr = original_stderr
        print("YT-DLP ERROR:", repr(e))
        raise


# encoding to meet openai api's img expectations
def encode_frame(frame):
    success, buffer = cv2.imencode(".jpg", frame)

    if not success:
        return None

    return base64.b64encode(buffer.tobytes()).decode("utf-8")


# async to reduce execution time
async def process_frames_async(video_path):
    loop = asyncio.get_event_loop()
    return await loop.run_in_executor(executor, process_frames, video_path)


def score_frame(frame):
    hf_token = HF_TOKEN.value
    clip_model, clip_processor = get_clip(hf_token)

    if clip_model is None or clip_processor is None:
            return 0.0

    image = Image.fromarray(cv2.cvtColor(frame, cv2.COLOR_BGR2RGB))

    prompts = [
        "news broadcast",
        "person speaking",
        "text on screen",
        "chart or graph",
        "interview",
        "headline screenshot"
    ]

    inputs = clip_processor(
        text=prompts,
        images=image,
        return_tensors="pt",
        padding=True
    )

    outputs = clip_model(**inputs)
    logits_per_image = outputs.logits_per_image
    scores = logits_per_image.softmax(dim=1)

    return scores.max().item()  # best match score


# only loading clip when actually will be used
def get_clip(hf_token):
    global clip_model, clip_processor

    if clip_model is None:
        try:
            clip_model = CLIPModel.from_pretrained(
                "openai/clip-vit-base-patch32",
                token=hf_token,
                cache_dir="/tmp/hf_models"
            )
            clip_processor = CLIPProcessor.from_pretrained(
                "openai/clip-vit-base-patch32",
                token=hf_token,
                cache_dir="/tmp/hf_models"
            )
        except Exception as e:
            print("[CLIP LOAD ERROR]:", e)
            return None, None

    return clip_model, clip_processor
# firebase deploy --only functions

def get_home_news(req: https_fn.CallableRequest):
    if req.auth is None:
        raise https_fn.HttpsError(
            code=https_fn.FunctionsErrorCode.UNAUTHENTICATED,
            message="User must be signed in."
        )

    data = req.data or {}
    topic = str(data.get("topic", "All")).strip()
    search = str(data.get("search", "")).strip()

    headers = {
        "X-Api-Key": NEWS_API_KEY.value
    }

    try:
        if search:
            response = requests.get(
                "https://newsapi.org/v2/everything",
                headers=headers,
                params={
                    "q": search,
                    "language": "en",
                    "sortBy": "publishedAt",
                    "pageSize": 10
                },
                timeout=20
            )
        elif topic == "Business":
            response = requests.get(
                "https://newsapi.org/v2/top-headlines",
                headers=headers,
                params={
                    "country": "us",
                    "category": "business",
                    "pageSize": 10
                },
                timeout=20
            )
        elif topic == "Tech":
            response = requests.get(
                "https://newsapi.org/v2/top-headlines",
                headers=headers,
                params={
                    "country": "us",
                    "category": "technology",
                    "pageSize": 10
                },
                timeout=20
            )
        elif topic == "International":
            response = requests.get(
                "https://newsapi.org/v2/everything",
                headers=headers,
                params={
                    "q": "international OR world news",
                    "language": "en",
                    "sortBy": "publishedAt",
                    "pageSize": 10
                },
                timeout=20
            )
        elif topic == "Politics":
            response = requests.get(
                "https://newsapi.org/v2/everything",
                headers=headers,
                params={
                    "q": "politics OR government OR election",
                    "language": "en",
                    "sortBy": "publishedAt",
                    "pageSize": 10
                },
                timeout=20
            )
        else:
            response = requests.get(
                "https://newsapi.org/v2/top-headlines",
                headers=headers,
                params={
                    "country": "us",
                    "pageSize": 10
                },
                timeout=20
            )

        payload = response.json()

        if response.status_code >= 400 or payload.get("status") != "ok":
            raise Exception(payload.get("message", f"HTTP {response.status_code}"))

        articles = []
        for article in payload.get("articles", []):
            articles.append({
                "title": article.get("title") or "Untitled",
                "imageUrl": article.get("urlToImage") or "",
                "url": article.get("url") or "",
                "source": (article.get("source") or {}).get("name") or "Unknown"
            })

        return {"articles": articles}

    except Exception as e:
        raise https_fn.HttpsError(
            code=https_fn.FunctionsErrorCode.INTERNAL,
            message=f"Failed to fetch home news: {str(e)}"
        )

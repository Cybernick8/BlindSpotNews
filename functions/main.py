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
import sys
import io

initialize_app()

executor = ThreadPoolExecutor()

OPENAI_API_KEY = SecretParam("OPENAI_API_KEY")
SUPADATA_API_KEY = SecretParam("SUPADATA_API_KEY")
NEWS_API_KEY = SecretParam("NEWS_API_KEY")
HF_TOKEN = SecretParam("HF_TOKEN")

SYSTEM_PROMPT = """
You are a fact and bias checking assistant for articles and transcripts.

Your goal is NOT to argue or make claims.
Your goal is to evaluate claims using verifiable evidence, scientific consensus, and data.

You must prioritize:
- empirical evidence
- measurable data
- scientific models
- widely accepted consensus

CRITICAL DIRECTIVE:
Most standard news reporting (like AP, Reuters, or local crime reports) contains ZERO bias and ZERO fake news.
If an article is standard, objective reporting, it is a MASSIVE SUCCESS to return an EMPTY list for "issues".
Do NOT nitpick standard journalistic terminology. Do NOT force findings. Only flag text if it is blatantly hyper-partisan, emotionally manipulative, or verifiably false."""

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

                # NOTE: May need to comment out until cookies are added, if receiving 403 Bot Blockers from YouTube
                video_task = loop.run_in_executor(executor, download_video, url)

                transcript_task = asyncio.create_task(fetch_transcript_async(url))

                if video_task:
                    try:
                        video_path = await video_task
                    except Exception as e:
                        print("[Download Error]:", repr(e))
                        video_path = None
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
                if not transcript:
                    text = "There seems to be an issue with analyzing this link. Please try again or use a different link."
                else:
                    text = transcript

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


        print("[IMG PIPELINE] Images sent: ", len(encoded_frames))

        for img in encoded_frames:
            if not img:
                continue
            content.append({
                "type": "input_image",
                "image_url": f"data:image/jpeg;base64,{img}"
            })

        resp = client.responses.create(
            model="gpt-5.4-mini",
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
        issues = verify_and_fix_issues(text, analysis.get("issues", []))
        image_issues = analysis.get("image_issues", [])
        bias_score = analysis.get("bias_score", 0)
        alignment = analysis.get("alignment", "Center")
        overall_analysis = analysis.get("summary", "No issues found.")
        print(f"\n\n[OUTPUT] issues: {issues}")
        print(f"\n\n[OUTPUT] vid issues: {image_issues}")
        print(f"\n\n[OUTPUT] summary: {overall_analysis }")
        print(f"\n\n[OUTPUT] bias score: {bias_score}")
        print(f"\n\n[OUTPUT] alignment: {alignment}")

        # Filtering out the frames we dont use in image_issues to reduce
        # data sent to client
        used_indices = sorted(set(
            int(issue["frame_index"])
            for issue in image_issues
            if "frame_index" in issue
            and isinstance(issue["frame_index"], (int, float))
            and int(issue["frame_index"]) < len(encoded_frames)
        ))

        filtered_frames = [
            encoded_frames[i]
            for i in used_indices
            if i < len(encoded_frames)
        ]

        index_map = {old: new for new, old in enumerate(used_indices)}

        for issue in image_issues:
            if "frame_index" in issue:
                old_index = int(issue["frame_index"])
                if old_index in index_map:
                    issue["frame_index"] = index_map[old_index]

        return {
            "text": text,
            "issues": issues,
            "image_issues": image_issues,
            "frames": filtered_frames,
            "overall_analysis": overall_analysis,
            "bias_score": bias_score,
            "alignment": alignment
        }

    except https_fn.HttpsError as e:
        raise e

    except Exception as e:
        print("[OPENAI ERROR RAW]:", repr(e))
        raise https_fn.HttpsError(
            code=https_fn.FunctionsErrorCode.INTERNAL,
            message=f"OpenAI request failed: {repr(e)}"
        )

def verify_and_fix_issues(text: str, issues: list) -> list:
    fixed = []
    for issue in issues:
        quote = issue.get("quote", "")
        start = issue.get("start")
        end = issue.get("end")

        if quote:
            if (
                start is not None and end is not None
                and 0 <= start < end <= len(text)
                and text[start:end] == quote
            ):
                fixed.append(issue)
                continue

            found = text.find(quote)
            if found != -1:
                issue["start"] = found
                issue["end"] = found + len(quote)
                fixed.append(issue)
            else:
                lower_text = text.lower()
                lower_quote = quote.lower()
                found_ci = lower_text.find(lower_quote)
                if found_ci != -1:
                    issue["start"] = found_ci
                    issue["end"] = found_ci + len(quote)
                    fixed.append(issue)
        elif start is not None and end is not None and 0 <= start < end <= len(text):
            fixed.append(issue)

    return fixed

def build_user_prompt(text: str) -> str:
    return f"""
You are performing a factual accuracy and political bias analysis of a transcript and/or related images.

STRICT RULES:
- Only report issues if there is clear evidence of:
  (1) verifiably false factual claims
  (2) meaningful political bias
- Do NOT speculate.
- Do NOT force findings.
- Only identify issues that directly relate to the content.
EVIDENCE REQUIREMENTS:
- Every issue and image issue MUST include concrete evidence or scientific reasoning.
- Prefer:
  - statistics (percentages, measurements, scale comparisons)
  - known scientific principles (physics, astronomy, biology, etc.)
  - real-world constraints (distances, speeds, forces, time scales)

- If evidence cannot be provided, DO NOT flag the issue.

- DO NOT respond with generic rebuttals

- INSTEAD respond with measurable contradictions or observable data conflicts

OUTPUT REQUIREMENTS:
- Return STRICTLY valid JSON (no markdown, no extra text).
- Include ALL issues found. Do not stop at one.
- There is NO limit to number of issues.
- If no issues exist, return empty arrays.

TEXT ANALYSIS:
- When reporting issues, you MUST include:
  - "quote": copy the EXACT verbatim text being flagged directly from the transcript (word-for-word, no changes)
  - "start": the character index of the first character of the quote within the transcript
  - "end": the character index immediately after the last character of the quote
- Indices are based on the EXACT transcript provided below.
- The "quote" field is mandatory. It must be a verbatim substring of the transcript.

IMAGE ANALYSIS:
- You are allowed to flag images that are:
  - misleading
  - emotionally manipulative
  - contextually deceptive
  - visually exaggerated
  - implying claims not supported by the transcript

- Examples of valid image issues:
 - dramatic, exaggerated visuals unrelated to actual claims
 - charts or graphics that are incorrect or being misrepresented
 - staged or sensational imagery

- Do NOT create more than 5 images analyses. If you find more than 5 image issues, only use the top 5 most relevant images that are UNIQUE from each other
- frame_index = index of image in input (0 = first image)
- Do NOT include start/end for image issues
- Refer to them as part of the video, not images. i.e., "In this part of the video..."

SUMMARY:
- Give a modest overall evaluation on the quality/credibility of the given source
- Try to go easy on subject
- if there are only speculative / lack of credible source issues, do not immediately assume incorrectness
- Try to explain both what is done well in the video and what is not

ID RULES:
- issues: ISSUE_1, ISSUE_2, ISSUE_3...
- image_issues: IMG_1, IMG_2, IMG_3...

JSON FORMAT:

{{
  "issues": [
    {{
      "id": "ISSUE_1",
      "type": "left | right | fake",
      "quote": "the exact verbatim text being flagged, copied directly from the transcript",
      "start": number,
      "end": number,
      "explanation": "clear explanation tied to the text span"
    }}
  ],
  "image_issues": [
    {{
      "id": "IMG_1",
      "frame_index": number,
      "type": "left | right | fake | misleading",
      "explanation": "clear explanation of the issue in the image"
    }}
  ],
  "summary": "2-6 sentence summary of the content.",
  "bias_score": number (1-10),
  "alignment": "Left | Lean Left | Center | Lean Right | Right"
}}

If no issues:

{{
  "text": "the full text of the article/transcript",
  "issues": [],
  "summary": "A 2-3 sentence explanation of the article's general tone, factual reliability, and why you did or did not detect bias.",
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

    # --- Using \n\n for paragraph breaks ---
    article = soup.find("article")
    if article:
        text = article.get_text("\n\n", strip=True)
    else:
        paragraphs = soup.find_all("p")
        text = "\n\n".join(p.get_text(" ", strip=True) for p in paragraphs)

    return text.strip()

# grabbing transcript for video, scrapping unnecessary content

async def fetch_transcript_async(url):
    supadata_endpoint = "https://api.supadata.ai/v1/transcript"

    def blocking_request():
        return requests.get(
            supadata_endpoint,
            params={"url": url},
            headers={"x-api-key": SUPADATA_API_KEY.value},
            timeout=120,
        )

    loop = asyncio.get_event_loop()
    r = await loop.run_in_executor(executor, blocking_request)

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
    fps = cap.get(cv2.CAP_PROP_FPS)

    if total_frames <= 0 or fps <= 0:
        cap.release()
        return []

    min_frames = 3
    max_frames = 20

    # sample ~15 candidate frames from middle
    duration_sec = total_frames / fps
    start = int(total_frames * 0.1)
    end = int(total_frames * 0.9)

    sample_count = int(min_frames + (duration_sec**2)/90)
    sample_count = min(sample_count, max_frames)


    indices = [
        int(start + (end - start) * i / (sample_count - 1))
        for i in range(sample_count)
    ]

    frames = []

    for idx in indices:
        cap.set(cv2.CAP_PROP_POS_FRAMES, idx)
        ret, frame = cap.read()

        if ret:
            frames.append(frame)

    cap.release()

    return [encode_frame(f) for f in frames if f is not None]


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

    original_stdout = sys.stdout
    original_stderr = sys.stderr

    try:
        sys.stdout = io.StringIO()
        sys.stderr = io.StringIO()

        with yt_dlp.YoutubeDL(ydl_opts) as ydl:
            result = ydl.extract_info(url, download=True)

        if not result:
            raise Exception("yt_dlp returned no result")

        file_path = ydl.prepare_filename(result)

        if isinstance(file_path, bytes):
            file_path = file_path.decode("utf-8", errors="ignore")

        return file_path

    except Exception as e:
        print("YT-DLP ERROR:", repr(e))
        return None   # ✅ IMPORTANT: don't re-raise

    finally:
        sys.stdout = original_stdout
        sys.stderr = original_stderr


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

@https_fn.on_call(
    secrets=[NEWS_API_KEY],
    memory=512
)
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

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

initialize_app()

executor = ThreadPoolExecutor()

OPENAI_API_KEY = SecretParam("OPENAI_API_KEY")
SUPADATA_API_KEY = SecretParam("SUPADATA_API_KEY")
NEWS_API_KEY = SecretParam("NEWS_API_KEY")

SYSTEM_PROMPT = "You are a fact and bias checking assistant for articles and transcripts."


# main function
# /////////////
# verify authentification, then for video:
#   download video and generate transcript grab relevant frames
#   from vid, upload transcript and frames to openai and wait
#   for response
# for article:
#   grab html, filter out unnecessary tags, and upload to openai
@https_fn.on_call(secrets=[OPENAI_API_KEY, SUPADATA_API_KEY])
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
                if url.endswith((".mp4", ".mov", ".avi", ".webm")):
                    video_task = loop.run_in_executor(executor, download_video, url)

                transcript_task = asyncio.create_task(fetch_transcript_async(url))

                if video_task:
                    video_path = await video_task
                else:
                    video_path = None

                if video_path:
                    frames_task = asyncio.create_task(process_frames_async(video_path))
                else:
                    frames_task = asyncio.create_task(asyncio.sleep(0, result=[]))

                transcript = await transcript_task
                frames = await frames_task

                end_video_pipeline = time.perf_counter()
                print(f"[PERFORMANCE] full vid pipeline: {end_video_pipeline - start_video_pipeline:.2f} sec")

                return transcript, frames

            transcript, encoded_frames = asyncio.run(run_video_pipeline())
            text = transcript

        else:
            r = requests.get(url, timeout=30, headers={"User-Agent": "Mozilla/5.0"})
            if r.status_code >= 400 or not r.text:
                raise Exception(f"Article fetch HTTP {r.status_code}")

            text = extract_text_from_html(r.text)
            encoded_frames = []

        if not text or len(text) < 50:
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

        return {
            "text": text,
            "issues": analysis.get("issues", []),
            "bias_score": analysis.get("bias_score", 0),
            "alignment": analysis.get("alignment", "Center")
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

IMPORTANT:
When reporting issues, return CHARACTER POSITIONS within the transcript.

The "start" value must be the index of the first character of the problematic text.
The "end" value must be the index immediately after the final character.

Indices are based on the EXACT transcript provided below.

Return VALID JSON ONLY.

JSON FORMAT:

{{
  "issues": [
    {{
      "id": "ISSUE_1",
      "type": "left | right | fake",
      "start": number,
      "end": number,
      "explanation": "brief explanation"
    }}
  ],
  "bias_score": number from 0-10,
  "alignment": "Left | Lean Left | Center | Lean Right | Right"
}}

If there are no issues:

{{
  "issues": [],
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
    # dumbed down to deal with cv2
    return []


# video -> usable frames (stub)
def extract_frames(video_path):
    return []


# Taking our video from url, downloading the video,
# and saving it
def download_video(url):
    response = requests.get(url, stream=True, timeout=30)

    file_path = f"/tmp/{uuid.uuid4()}.mp4"

    with open(file_path, "wb") as f:
        for chunk in response.iter_content(chunk_size=1024):
            if chunk:
                f.write(chunk)

    return file_path


# encoding to meet openai api's img expectations
def encode_frame(frame):
    return ""


# async to reduce execution time
async def process_frames_async(video_path):
    loop = asyncio.get_event_loop()
    return await loop.run_in_executor(executor, process_frames, video_path)

# firebase deploy --only functions
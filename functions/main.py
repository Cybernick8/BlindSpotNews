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

initialize_app()


OPENAI_API_KEY = SecretParam("OPENAI_API_KEY")
SUPADATA_API_KEY = SecretParam("SUPADATA_API_KEY")

SYSTEM_PROMPT = "You are a fact and bias checking assistant for articles and transcripts."

def extract_text_from_html(html: str) -> str:
    soup = BeautifulSoup(html, "html.parser")

    # Remove noisy elements similar to what you did with Jsoup
    for tag in soup(["script", "style", "nav", "footer", "header", "iframe", "form", "input",
                     "button", "canvas", "svg", "video", "audio", "link", "meta", "noscript"]):
        tag.decompose()

    # Prefer <article> if present, else fall back to paragraphs
    article = soup.find("article")
    if article:
        text = article.get_text(" ", strip=True)
    else:
        paragraphs = soup.find_all("p")
        text = " ".join(p.get_text(" ", strip=True) for p in paragraphs)

    return text.strip()

def build_user_prompt(text: str) -> str:
    return f"""
You are performing a factual and political bias review of a transcript.

STRICT RULES:
- Only report issues if there is clear evidence of:
  (1) verifiably false factual claims, or
  (2) meaningful political bias (loaded framing or major contextual omission).
- Do NOT speculate.
- Do NOT force findings.
- If the transcript contains no political content or no clear factual errors, output exactly:

NO_ISSUES_FOUND
Bias: 0/10
Center

and nothing else.

FORMAT REQUIREMENTS:

For each issue found, you MUST wrap it exactly between these markers:

/~BLINDSPOT_ISSUE_START~/
ID: ISSUE_<incrementing number starting at 1>
TYPE: left | right | fake
QUOTE: "<exact quoted text from transcript>"
EXPLANATION: <brief explanation>
/~BLINDSPOT_ISSUE_END~/

After all issues (if any), output:

Bias: X/10
Political Alignment: Left | Lean Left | Center | Lean Right | Right

Transcript:
{text}
""".strip()



@https_fn.on_call(secrets=[OPENAI_API_KEY, SUPADATA_API_KEY])
def analyze_url(req: https_fn.CallableRequest):
    # Require authentication (matches your earlier security intent)
    print("Auth object:", req.auth)
    if req.auth is None:
        raise https_fn.HttpsError(
            code=https_fn.FunctionsErrorCode.UNAUTHENTICATED,
            message="User must be signed in."
        )

    data = req.data or {}
    url = str(data.get("url", "")).strip()
    is_video = bool(data.get("isVideo", False))

    if not url:
        raise https_fn.HttpsError(
            code=https_fn.FunctionsErrorCode.INVALID_ARGUMENT,
            message="url is required."
        )

    # Get text: transcript or article content
    try:
        if is_video:
            # Supadata transcript endpoint
            supadata_endpoint = "https://api.supadata.ai/v1/transcript"
            r = requests.get(
                supadata_endpoint,
                params={"url": url},
                headers={"x-api-key": SUPADATA_API_KEY.value},
                timeout=60,
            )
            if r.status_code >= 400:
                raise Exception(f"Supadata HTTP {r.status_code}: {r.text[:200]}")
            text = r.text  # keep raw JSON/text
        else:
            r = requests.get(url, timeout=30, headers={"User-Agent": "Mozilla/5.0"})
            if r.status_code >= 400 or not r.text:
                raise Exception(f"Article fetch HTTP {r.status_code}")
            text = extract_text_from_html(r.text)

        if not text or len(text) < 50:
            raise Exception("No usable text extracted.")
    except Exception as e:
        raise https_fn.HttpsError(
            code=https_fn.FunctionsErrorCode.INTERNAL,
            message=f"Failed to retrieve content: {e}"
        )

    # Capping tokens potentially? TODO: Find good number for this
    text = text[:12000]

    # OpenAI
    try:
        client = OpenAI(api_key=OPENAI_API_KEY.value)
        user_prompt = build_user_prompt(text)

        resp = client.responses.create(
            model="gpt-4.1-mini",
            input=[
                {"role": "system", "content": SYSTEM_PROMPT},
                {"role": "user", "content": user_prompt},
            ],
        )

        return resp.output_text
    except Exception as e:
        raise https_fn.HttpsError(
            code=https_fn.FunctionsErrorCode.INTERNAL,
            message=f"OpenAI request failed: {e}"
        )

# firebase deploy --only functions
import os
import json
import base64
import asyncio
from fastapi import FastAPI, HTTPException, Request, Response, UploadFile, File, Form
from fastapi.responses import StreamingResponse, FileResponse, PlainTextResponse
from fastapi.staticfiles import StaticFiles
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from typing import Optional, List, Dict, Any

import database as db
from model_manager import manager, MODEL_CONFIGS, PERSONAS
from model_engine import model_engine, PRELOADED_RULES

# Try importing PDF reader
try:
    import pymupdf  # PyMuPDF
    HAS_PYMUPDF = True
except Exception:
    HAS_PYMUPDF = False

app = FastAPI(title="K2 Horizon Chat")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Initialize database
db.init_db()

# Serve static files and documentation
app.mount("/static", StaticFiles(directory="static"), name="static")
if os.path.exists("docs"):
    app.mount("/docs", StaticFiles(directory="docs"), name="docs")

class CreateChatRequest(BaseModel):
    model: str # "0.9b" or "7b"
    title: Optional[str] = "New Conversation"
    persona: Optional[str] = "default"

class UpdateChatRequest(BaseModel):
    title: Optional[str] = None
    persona: Optional[str] = None

class SendMessageRequest(BaseModel):
    content: str
    attachments: Optional[List[Dict[str, Any]]] = []

class EditMessageRequest(BaseModel):
    content: str
    attachments: Optional[List[Dict[str, Any]]] = []

@app.get("/")
async def root():
    return FileResponse("static/index.html")

@app.get("/blog")
@app.get("/blog.html")
async def blog():
    return FileResponse("static/blog.html")

@app.get("/post")
@app.get("/post.html")
@app.get("/journey")
async def post_page():
    return FileResponse("static/post.html")

@app.get("/test")
@app.get("/test.html")
async def test_page():
    return FileResponse("static/test.html")

class TestEvaluateRequest(BaseModel):
    model: str = "both" # "k2", "llama", "both"
    rules: str
    app: str = "WhatsApp"
    sender: str = "Rahul"
    text: str = ""
    is_call: Optional[bool] = False

# Load Test Cases (prefer test_cases_500.json, fallback to test_cases_1000.json)
TEST_CASES_DATA = []
target_dataset_file = "test_cases_500.json" if os.path.exists("test_cases_500.json") else "test_cases_1000.json"
if os.path.exists(target_dataset_file):
    try:
        with open(target_dataset_file, "r", encoding="utf-8") as f:
            TEST_CASES_DATA = json.load(f)
        print(f"[Server] Loaded {len(TEST_CASES_DATA)} cases from {target_dataset_file}")
    except Exception as e:
        print(f"[Server] Error loading test cases: {e}")

@app.get("/api/test/models")
async def get_test_models():
    return model_engine.get_models_status()

@app.get("/api/test/rules")
async def get_test_rules():
    compiled_data = None
    if os.path.exists("compiled_rules_phase1.json"):
        try:
            with open("compiled_rules_phase1.json", "r", encoding="utf-8") as f:
                compiled_data = json.load(f)
        except Exception:
            pass
    return {
        "rules": PRELOADED_RULES,
        "compiled": compiled_data
    }

@app.get("/api/test/benchmark-results")
async def get_benchmark_results():
    if os.path.exists("phase2_benchmark_500_results.json"):
        try:
            with open("phase2_benchmark_500_results.json", "r", encoding="utf-8") as f:
                return json.load(f)
        except Exception as e:
            return {"error": str(e)}
    return {"status": "not_run_yet"}

@app.get("/api/test/cases")
async def get_test_cases(
    group: Optional[str] = None,
    rule_id: Optional[str] = None,
    action: Optional[str] = None,
    search: Optional[str] = None,
    limit: Optional[int] = 1000,
    offset: Optional[int] = 0
):
    filtered = TEST_CASES_DATA
    if group:
        filtered = [c for c in filtered if group.lower() in c.get("group", "").lower()]
    if rule_id:
        filtered = [c for c in filtered if c.get("rule_id") == rule_id]
    if action:
        filtered = [c for c in filtered if c.get("ground_truth_action", "").upper() == action.upper()]
    if search:
        s = search.lower()
        filtered = [
            c for c in filtered 
            if s in c.get("text", "").lower() or s in c.get("sender", "").lower() or s in c.get("app", "").lower() or s in c.get("ground_truth_reason", "").lower()
        ]
    
    total = len(filtered)
    paginated = filtered[offset : offset + limit] if limit else filtered
    return {
        "total": total,
        "count": len(paginated),
        "offset": offset,
        "cases": paginated
    }

class CompileRulesRequest(BaseModel):
    model: str = "both" # "k2", "llama", "both"
    rules: Optional[List[str]] = None

@app.post("/api/test/compile-rules")
async def compile_rules_endpoint(payload: CompileRulesRequest):
    """
    Sends natural language rules to K2 Horizon and Llama 3.2 to classify & compile
    into structured JSON schemas (SIMPLE_RULE vs COMPLEX_RULE, category, tone, etc.).
    """
    target_rules = payload.rules if payload.rules else PRELOADED_RULES
    model_choice = payload.model.lower()

    async def event_generator():
        yield f"event: compile_start\ndata: {json.dumps({'total': len(target_rules), 'model': model_choice})}\n\n"

        for idx, r_text in enumerate(target_rules):
            yield f"event: rule_compile_progress\ndata: {json.dumps({'index': idx + 1, 'total': len(target_rules), 'rule_text': r_text})}\n\n"

            k2_res = None
            llama_res = None

            # 1. Compile with K2
            if model_choice in ["k2", "both"]:
                k2_prompt = model_engine.build_compile_rule_prompt_chatml(r_text)
                async for chunk in model_engine.stream_k2_inference(k2_prompt):
                    if chunk["type"] == "final":
                        k2_res = chunk

            # 2. Compile with Llama
            if model_choice in ["llama", "both"]:
                llama_prompt = model_engine.build_compile_rule_prompt_llama(r_text)
                async for chunk in model_engine.stream_llama_inference(llama_prompt):
                    if chunk["type"] == "final":
                        llama_res = chunk

            rule_result = {
                "rule_index": idx + 1,
                "rule_text": r_text,
                "k2": {
                    "schema": k2_res.get("parsed_json") if k2_res else None,
                    "is_valid_json": k2_res.get("is_valid_json", False) if k2_res else False,
                    "raw_output": k2_res.get("raw_output", "") if k2_res else "",
                    "metrics": k2_res.get("metrics") if k2_res else None
                } if k2_res else None,
                "llama": {
                    "schema": llama_res.get("parsed_json") if llama_res else None,
                    "is_valid_json": llama_res.get("is_valid_json", False) if llama_res else False,
                    "raw_output": llama_res.get("raw_output", "") if llama_res else "",
                    "metrics": llama_res.get("metrics") if llama_res else None
                } if llama_res else None
            }

            yield f"event: rule_compile_result\ndata: {json.dumps(rule_result)}\n\n"

        yield f"event: compile_complete\ndata: {json.dumps({'status': 'complete', 'total': len(target_rules)})}\n\n"

    return StreamingResponse(
        event_generator(),
        media_type="text/event-stream",
        headers={
            "Cache-Control": "no-cache",
            "Connection": "keep-alive",
            "X-Accel-Buffering": "no"
        }
    )

class BatchRunRequest(BaseModel):
    model: str = "both" # "k2", "llama", "both"
    test_case_ids: Optional[List[int]] = None
    limit: Optional[int] = 10
    group: Optional[str] = None
    rule_id: Optional[str] = None

@app.post("/api/test/batch-run")
async def run_batch_benchmark(payload: BatchRunRequest):
    """
    Executes real-time batch benchmark across local models on selected test cases.
    Streams progress, decisions, accuracy match, and scorecard.
    """
    # Filter target cases
    target_cases = TEST_CASES_DATA
    if payload.test_case_ids:
        id_set = set(payload.test_case_ids)
        target_cases = [c for c in target_cases if c["id"] in id_set]
    elif payload.group:
        target_cases = [c for c in target_cases if payload.group.lower() in c.get("group", "").lower()]
    elif payload.rule_id:
        target_cases = [c for c in target_cases if c.get("rule_id") == payload.rule_id]
    
    if payload.limit and payload.limit > 0:
        target_cases = target_cases[:payload.limit]

    total_cases = len(target_cases)
    model_choice = payload.model.lower()

    async def batch_generator():
        k2_correct = 0
        llama_correct = 0
        k2_total_time = 0.0
        llama_total_time = 0.0
        processed_count = 0

        yield f"event: batch_start\ndata: {json.dumps({'total': total_cases, 'model': model_choice})}\n\n"

        for idx, tc in enumerate(target_cases):
            processed_count += 1
            case_id = tc["id"]
            gt_action = tc["ground_truth_action"] # "ALERT" or "MUTE"
            
            # Find the corresponding rule text
            r_idx = 0
            if tc["rule_id"].startswith("rule_"):
                try:
                    r_idx = int(tc["rule_id"].split("_")[1]) - 1
                except Exception:
                    r_idx = 0
            rule_text = PRELOADED_RULES[r_idx] if 0 <= r_idx < len(PRELOADED_RULES) else PRELOADED_RULES[0]

            yield f"event: case_progress\ndata: {json.dumps({'index': idx + 1, 'total': total_cases, 'case_id': case_id, 'sender': tc['sender'], 'app': tc['app']})}\n\n"

            k2_res = None
            llama_res = None

            # 1. Run K2
            if model_choice in ["k2", "both"]:
                k2_prompt = model_engine.build_prompt_chatml(
                    rules=rule_text,
                    app_name=tc["app"],
                    sender=tc["sender"],
                    text=tc["text"],
                    is_call=tc.get("is_call", False)
                )
                async for chunk in model_engine.stream_k2_inference(k2_prompt):
                    if chunk["type"] == "final":
                        k2_res = chunk

            # 2. Run Llama
            if model_choice in ["llama", "both"]:
                llama_prompt = model_engine.build_prompt_llama(
                    rules=rule_text,
                    app_name=tc["app"],
                    sender=tc["sender"],
                    text=tc["text"],
                    is_call=tc.get("is_call", False)
                )
                async for chunk in model_engine.stream_llama_inference(llama_prompt):
                    if chunk["type"] == "final":
                        llama_res = chunk

            # Evaluate decision accuracy
            k2_decision = "MUTE"
            k2_is_correct = False
            if k2_res and k2_res.get("parsed_json"):
                pj = k2_res["parsed_json"]
                k2_decision = "ALERT" if (pj.get("alert") is True or pj.get("important") is True) else "MUTE"
                k2_is_correct = (k2_decision == gt_action)
                if k2_is_correct:
                    k2_correct += 1
                k2_total_time += k2_res["metrics"]["total_ms"]

            llama_decision = "MUTE"
            llama_is_correct = False
            if llama_res and llama_res.get("parsed_json"):
                pj = llama_res["parsed_json"]
                llama_decision = "ALERT" if (pj.get("alert") is True or pj.get("important") is True) else "MUTE"
                llama_is_correct = (llama_decision == gt_action)
                if llama_is_correct:
                    llama_correct += 1
                llama_total_time += llama_res["metrics"]["total_ms"]

            case_summary = {
                "case_id": case_id,
                "rule_name": tc["rule_name"],
                "group": tc["group"],
                "app": tc["app"],
                "sender": tc["sender"],
                "text": tc["text"],
                "ground_truth": gt_action,
                "ground_truth_reason": tc["ground_truth_reason"],
                "difficulty": tc.get("difficulty", "Medium"),
                "k2": {
                    "decision": k2_decision,
                    "correct": k2_is_correct,
                    "metrics": k2_res.get("metrics") if k2_res else None,
                    "reason": k2_res.get("parsed_json", {}).get("reason", "") if k2_res and k2_res.get("parsed_json") else "",
                    "raw_output": k2_res.get("raw_output", "") if k2_res else ""
                } if k2_res else None,
                "llama": {
                    "decision": llama_decision,
                    "correct": llama_is_correct,
                    "metrics": llama_res.get("metrics") if llama_res else None,
                    "reason": llama_res.get("parsed_json", {}).get("reason", "") if llama_res and llama_res.get("parsed_json") else "",
                    "raw_output": llama_res.get("raw_output", "") if llama_res else ""
                } if llama_res else None
            }

            yield f"event: case_result\ndata: {json.dumps(case_summary)}\n\n"

        # Final Scorecard
        scorecard = {
            "total_evaluated": processed_count,
            "k2": {
                "correct": k2_correct,
                "accuracy": round((k2_correct / processed_count) * 100, 1) if processed_count > 0 else 0,
                "avg_latency_ms": round(k2_total_time / processed_count, 1) if processed_count > 0 else 0
            },
            "llama": {
                "correct": llama_correct,
                "accuracy": round((llama_correct / processed_count) * 100, 1) if processed_count > 0 else 0,
                "avg_latency_ms": round(llama_total_time / processed_count, 1) if processed_count > 0 else 0
            }
        }
        yield f"event: batch_complete\ndata: {json.dumps(scorecard)}\n\n"

    return StreamingResponse(
        batch_generator(),
        media_type="text/event-stream",
        headers={
            "Cache-Control": "no-cache",
            "Connection": "keep-alive",
            "X-Accel-Buffering": "no"
        }
    )

@app.get("/api/status")
async def get_status():
    return manager.get_status()

@app.get("/api/chats")
async def list_chats():
    chats = db.get_chats()
    return {"chats": chats}

@app.post("/api/chats")
async def create_chat(payload: CreateChatRequest):
    if payload.model not in MODEL_CONFIGS:
        raise HTTPException(status_code=400, detail=f"Invalid model. Choose from: {list(MODEL_CONFIGS.keys())}")
    chat = db.create_chat(model=payload.model, title=payload.title or "New Conversation", persona=payload.persona or "default")
    return chat

@app.get("/api/chats/{chat_id}")
async def get_chat_detail(chat_id: str):
    chat = db.get_chat(chat_id)
    if not chat:
        raise HTTPException(status_code=404, detail="Chat not found")
    messages = db.get_messages(chat_id)
    return {"chat": chat, "messages": messages}

@app.patch("/api/chats/{chat_id}")
async def update_chat(chat_id: str, payload: UpdateChatRequest):
    chat = db.get_chat(chat_id)
    if not chat:
        raise HTTPException(status_code=404, detail="Chat not found")
    if payload.title is not None:
        db.update_chat_title(chat_id, payload.title)
    if payload.persona is not None:
        db.update_chat_persona(chat_id, payload.persona)
    return {"status": "ok", "chat": db.get_chat(chat_id)}

@app.delete("/api/chats/{chat_id}")
async def delete_chat(chat_id: str):
    chat = db.get_chat(chat_id)
    if not chat:
        raise HTTPException(status_code=404, detail="Chat not found")
    db.delete_chat(chat_id)
    return {"status": "deleted"}

@app.post("/api/upload")
async def upload_document(file: UploadFile = File(...)):
    """Extract text from uploaded documents (PDF, TXT, DOCX, Code, CSV, etc.) or process images"""
    try:
        filename = file.filename or "uploaded_file"
        ext = os.path.splitext(filename)[1].lower()
        contents = await file.read()
        size_kb = round(len(contents) / 1024, 1)

        extracted_text = ""
        file_type = "file"
        preview_data = None

        if ext == ".pdf":
            file_type = "pdf"
            if HAS_PYMUPDF:
                doc = pymupdf.open(stream=contents, filetype="pdf")
                pages_text = []
                for idx, page in enumerate(doc):
                    if idx >= 20: # cap at first 20 pages
                        pages_text.append(f"\n[... truncated {len(doc) - 20} more pages ...]")
                        break
                    p_txt = page.get_text().strip()
                    if p_txt:
                        pages_text.append(f"[Page {idx+1}]\n{p_txt}")
                extracted_text = "\n\n".join(pages_text)
                if not extracted_text:
                    extracted_text = "(PDF contains scanned images or non-selectable text)"
            else:
                extracted_text = f"(PDF reader not initialized for {filename})"

        elif ext in [".png", ".jpg", ".jpeg", ".webp", ".bmp", ".gif"]:
            file_type = "image"
            b64_img = base64.b64encode(contents).decode("utf-8")
            mime = "image/png" if ext == ".png" else "image/jpeg"
            preview_data = f"data:{mime};base64,{b64_img}"
            extracted_text = f"[Image Attached: {filename} ({size_kb} KB)]"

        else:
            file_type = "code" if ext in [".py", ".js", ".ts", ".html", ".css", ".cpp", ".c", ".rs", ".go", ".java", ".sql", ".sh", ".json", ".yaml", ".yml"] else "text"
            try:
                extracted_text = contents.decode("utf-8", errors="replace")
                if len(extracted_text) > 20000:
                    extracted_text = extracted_text[:20000] + "\n\n[... Truncated due to size limit ...]"
            except Exception as err:
                extracted_text = f"Error reading text content: {err}"

        return {
            "name": filename,
            "type": file_type,
            "size_kb": size_kb,
            "text": extracted_text,
            "preview": preview_data
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Failed to process upload: {str(e)}")

@app.post("/api/chats/{chat_id}/message")
async def send_message(chat_id: str, payload: SendMessageRequest):
    chat = db.get_chat(chat_id)
    if not chat:
        raise HTTPException(status_code=404, detail="Chat not found")
    if not payload.content.strip() and not payload.attachments:
        raise HTTPException(status_code=400, detail="Empty message")

    user_msg = db.add_message(
        chat_id=chat_id,
        role="user",
        content=payload.content.strip() or "[Sent attachment]",
        attachments=payload.attachments or []
    )
    
    # Auto-generate title if this is the first message
    messages = db.get_messages(chat_id)
    if len(messages) <= 1:
        auto_title = payload.content.strip()[:35] + ("..." if len(payload.content.strip()) > 35 else "")
        if not auto_title and payload.attachments:
            auto_title = f"Document: {payload.attachments[0].get('name', 'File')}"
        db.update_chat_title(chat_id, auto_title or "New Conversation")

    return {"message": user_msg}

@app.post("/api/chats/{chat_id}/abort")
async def abort_chat_stream(chat_id: str):
    """Abort running inference stream"""
    manager.abort_stream(chat_id)
    return {"status": "aborted"}

@app.post("/api/chats/{chat_id}/regenerate/{message_id}")
async def regenerate_from_message(chat_id: str, message_id: str):
    """Delete messages starting from assistant response and re-run"""
    chat = db.get_chat(chat_id)
    if not chat:
        raise HTTPException(status_code=404, detail="Chat not found")
    db.delete_messages_after(chat_id, message_id)
    return {"status": "ok"}

@app.get("/api/chats/{chat_id}/export")
async def export_chat(chat_id: str, format: str = "markdown"):
    chat = db.get_chat(chat_id)
    if not chat:
        raise HTTPException(status_code=404, detail="Chat not found")
    messages = db.get_messages(chat_id)

    if format == "json":
        return {"chat": chat, "messages": messages}
    
    # Format markdown
    md_lines = [
        f"# {chat.title}",
        f"*Model: {chat['model']} | Created: {chat['created_at']}*",
        "\n---\n"
    ]
    for m in messages:
        role_title = "👤 User" if m["role"] == "user" else "🤖 K2 Horizon"
        md_lines.append(f"### {role_title}\n")
        if m.get("thinking"):
            md_lines.append(f"> **Thought Process**:\n> {m['thinking'].replace(chr(10), chr(10)+'> ')}\n")
        md_lines.append(m["content"] + "\n")
        if m.get("metrics") and m["metrics"].get("tok_per_sec"):
            met = m["metrics"]
            md_lines.append(f"*\\[Latency: {met.get('ttft_ms',0)}ms | Speed: {met.get('tok_per_sec',0)} tok/s\\]*\n")
        md_lines.append("\n---\n")

    md_content = "\n".join(md_lines)
    return PlainTextResponse(
        md_content,
        headers={"Content-Disposition": f'attachment; filename="k2_chat_{chat_id[:8]}.md"'}
    )

@app.get("/api/chats/{chat_id}/stream")
async def stream_chat_response(chat_id: str):
    chat = db.get_chat(chat_id)
    if not chat:
        raise HTTPException(status_code=404, detail="Chat not found")

    messages = db.get_messages(chat_id)
    if not messages:
        raise HTTPException(status_code=400, detail="No messages in conversation")

    persona = chat.get("persona", "default") or "default"

    async def event_generator():
        thinking_text = ""
        answer_text = ""
        final_metrics = {}

        try:
            async for sse_event in manager.stream_chat(model_key=chat["model"], messages=messages, chat_id=chat_id, persona=persona):
                yield sse_event

                # Intercept metrics to persist on completion
                if sse_event.startswith("event: metrics"):
                    lines = sse_event.strip().split("\n")
                    for line in lines:
                        if line.startswith("data: "):
                            try:
                                data = json.loads(line[6:])
                                final_metrics = data
                                thinking_text = data.get("full_thinking", "")
                                answer_text = data.get("full_answer", "")
                            except Exception:
                                pass

            # Save assistant message to database
            if answer_text or thinking_text:
                db.add_message(
                    chat_id=chat_id,
                    role="assistant",
                    content=answer_text or "*(Response completed with reasoning)*",
                    thinking=thinking_text,
                    metrics=final_metrics
                )
        except Exception as e:
            error_data = json.dumps({"error": str(e)})
            yield f"event: error\ndata: {error_data}\n\n"

    return StreamingResponse(
        event_generator(),
        media_type="text/event-stream",
        headers={
            "Cache-Control": "no-cache",
            "Connection": "keep-alive",
            "X-Accel-Buffering": "no"
        }
    )

if __name__ == "__main__":
    import uvicorn
    print("\n" + "="*60)
    print(" [K2 Horizon] ChatGPT-Style Web Server Running")
    print(" Access UI at: http://localhost:8000")
    print("="*60 + "\n")
    uvicorn.run("server:app", host="0.0.0.0", port=8000, reload=False)


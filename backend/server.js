import "dotenv/config";
import express from "express";
import cors from "cors";
import rateLimit from "express-rate-limit";
import { GoogleGenerativeAI } from "@google/generative-ai";

const app = express();
app.use(cors()); // In production, restrict this to your app's known domain if applicable.
app.use(express.json({ limit: "20kb" }));
app.use(rateLimit({ windowMs: 60_000, limit: 20 }));

const apiKey = process.env.GEMINI_API_KEY;
if (!apiKey) console.warn("WARNING: GEMINI_API_KEY belum diatur.");

app.get("/", (_req, res) => res.json({ app: "Zyle AI backend", status: "ok" }));

app.post("/chat", async (req, res) => {
  try {
    const message = typeof req.body?.message === "string" ? req.body.message.trim() : "";
    if (!message) return res.status(400).json({ error: "Pesan tidak boleh kosong." });
    if (message.length > 8000) return res.status(413).json({ error: "Pesan terlalu panjang." });
    if (!apiKey) return res.status(503).json({ error: "API key belum dikonfigurasi di server." });

    const genAI = new GoogleGenerativeAI(apiKey);
    const model = genAI.getGenerativeModel({
      model: "gemini-2.5-flash",
      systemInstruction: "Namamu Zyle AI. Jawab dalam bahasa Indonesia dengan ramah. Bantu belajar dan coding secara aman. Jangan mengklaim telah melakukan pencarian web langsung jika tidak menggunakan alat pencarian."
    });
    const result = await model.generateContent(message);
    res.json({ reply: result.response.text() });
  } catch (err) {
    console.error("AI request failed:", err?.message || err);
    res.status(502).json({ error: "AI gagal memproses permintaan. Coba lagi nanti." });
  }
});

const port = Number(process.env.PORT || 3000);
app.listen(port, "0.0.0.0", () => console.log(`Zyle AI backend listening on ${port}`));

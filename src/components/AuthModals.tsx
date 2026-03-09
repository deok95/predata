import React, { useState, useRef, useEffect } from "react";
import { COLORS } from "../theme";
import { authApi, setToken, googleSignIn } from "../services/api";

const decodeJwtPayload = (token) => {
  try {
    const [, payload] = token.split(".");
    if (!payload) return {};
    const normalized = payload.replace(/-/g, "+").replace(/_/g, "/");
    const padded = normalized + "=".repeat((4 - (normalized.length % 4)) % 4);
    const json = atob(padded);
    return JSON.parse(json);
  } catch {
    return {};
  }
};

function AuthModal({ children, onClose }) {
  const backdropRef = useRef(null);
  useEffect(() => {
    const h = (e) => { if (e.key === "Escape") onClose(); };
    document.addEventListener("keydown", h);
    document.body.style.overflow = "hidden";
    return () => { document.removeEventListener("keydown", h); document.body.style.overflow = ""; };
  }, [onClose]);

  return (
    <div ref={backdropRef} onClick={(e) => { if (e.target === backdropRef.current) onClose(); }} style={{
      position: "fixed", inset: 0, zIndex: 1000,
      background: "rgba(0,0,0,0.65)", backdropFilter: "blur(8px)",
      display: "flex", alignItems: "center", justifyContent: "center",
      padding: 24, animation: "modalFadeIn 0.2s ease both",
    }}>
      <div style={{
        width: "100%", maxWidth: 420,
        background: COLORS.surface, border: `1px solid ${COLORS.border}`,
        borderRadius: 20, padding: "36px 32px 28px",
        animation: "modalSlideUp 0.25s ease both",
        position: "relative",
      }}>
        <button onClick={onClose} style={{
          position: "absolute", top: 16, right: 16,
          background: "none", border: "none", cursor: "pointer",
          color: COLORS.textDim, padding: 4, display: "flex", borderRadius: 6,
          transition: "all 0.15s",
        }}
        onMouseEnter={e => { e.currentTarget.style.color = COLORS.text; e.currentTarget.style.background = "rgba(255,255,255,0.05)"; }}
        onMouseLeave={e => { e.currentTarget.style.color = COLORS.textDim; e.currentTarget.style.background = "none"; }}
        >
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round"><path d="M18 6L6 18M6 6l12 12"/></svg>
        </button>
        {children}
      </div>
    </div>
  );
}

function LoginModal({ onClose, onSwitchToSignup }) {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const [googlePending, setGooglePending] = useState<{ googleId: string; email: string } | null>(null);
  const [googleCountryCode, setGoogleCountryCode] = useState("KR");
  const [googleJobCategory, setGoogleJobCategory] = useState("");
  const [googleAgeGroup, setGoogleAgeGroup] = useState("");
  const [googleGender, setGoogleGender] = useState("");
  const [googleBirthDate, setGoogleBirthDate] = useState("");

  const handleLogin = async (e) => {
    e.preventDefault();
    if (!email || !password) { setError("Please enter your email and password."); return; }
    setError(""); setLoading(true);
    try {
      const res = await authApi.login(email, password);
      setToken(res.token ?? null);
      onClose();
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : "Login failed.");
    } finally {
      setLoading(false);
    }
  };

  const handleCompleteGoogleRegistration = async (e) => {
    e.preventDefault();
    if (!googlePending) return;
    if (!googleCountryCode.trim()) {
      setError("Country code is required.");
      return;
    }
    setError("");
    setLoading(true);
    try {
      const res = await authApi.googleCompleteRegistration({
        googleId: googlePending.googleId,
        email: googlePending.email,
        countryCode: googleCountryCode.trim().toUpperCase(),
        jobCategory: googleJobCategory.trim() || undefined,
        ageGroup: googleAgeGroup ? Number(googleAgeGroup) : undefined,
        gender: googleGender || undefined,
        birthDate: googleBirthDate || undefined,
      });
      setToken(res.token ?? null);
      onClose();
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : "Google registration failed.");
    } finally {
      setLoading(false);
    }
  };

  if (googlePending) {
    return (
      <AuthModal onClose={onClose}>
        <div style={{ textAlign: "center", marginBottom: 20 }}>
          <div style={{ fontSize: 22, fontWeight: 700, color: COLORS.text, fontFamily: "'Outfit', sans-serif" }}>
            Complete Google Sign-in
          </div>
          <div style={{ fontSize: 13, color: COLORS.textDim, marginTop: 8 }}>{googlePending.email}</div>
        </div>

        {error && (
          <div style={{ padding: "10px 14px", borderRadius: 10, marginBottom: 14, background: "rgba(239,68,68,0.08)", border: "1px solid rgba(239,68,68,0.12)", fontSize: 13, color: COLORS.red, fontWeight: 500 }}>{error}</div>
        )}

        <form onSubmit={handleCompleteGoogleRegistration}>
          <div style={{ border: `1.5px solid ${COLORS.border}`, borderRadius: 12, overflow: "hidden", marginBottom: 10 }}>
            <select
              value={googleCountryCode}
              onChange={e => setGoogleCountryCode(e.target.value)}
              style={{ width: "100%", padding: "14px 16px", border: "none", outline: "none", fontSize: 15, fontFamily: "'DM Sans', sans-serif", color: COLORS.text, background: "rgba(255,255,255,0.02)", appearance: "none" }}
            >
              <option value="KR">South Korea (KR)</option>
              <option value="US">United States (US)</option>
              <option value="JP">Japan (JP)</option>
              <option value="CN">China (CN)</option>
              <option value="GB">United Kingdom (GB)</option>
              <option value="DE">Germany (DE)</option>
              <option value="FR">France (FR)</option>
              <option value="SG">Singapore (SG)</option>
            </select>
          </div>
          <div style={{ border: `1.5px solid ${COLORS.border}`, borderRadius: 12, overflow: "hidden", marginBottom: 10 }}>
            <select
              value={googleJobCategory}
              onChange={e => setGoogleJobCategory(e.target.value)}
              style={{ width: "100%", padding: "14px 16px", border: "none", outline: "none", fontSize: 15, fontFamily: "'DM Sans', sans-serif", color: COLORS.text, background: "rgba(255,255,255,0.02)", appearance: "none" }}
            >
              <option value="">Job category (optional)</option>
              <option value="Student">Student</option>
              <option value="IT">IT</option>
              <option value="Finance">Finance</option>
              <option value="Medical">Medical</option>
              <option value="Other">Other</option>
            </select>
          </div>
          <div style={{ border: `1.5px solid ${COLORS.border}`, borderRadius: 12, overflow: "hidden", marginBottom: 10 }}>
            <select
              value={googleAgeGroup}
              onChange={e => setGoogleAgeGroup(e.target.value)}
              style={{ width: "100%", padding: "14px 16px", border: "none", outline: "none", fontSize: 15, fontFamily: "'DM Sans', sans-serif", color: COLORS.text, background: "rgba(255,255,255,0.02)", appearance: "none" }}
            >
              <option value="">Age group (optional)</option>
              <option value="10">10s</option>
              <option value="20">20s</option>
              <option value="30">30s</option>
              <option value="40">40s</option>
              <option value="50">50s</option>
              <option value="60">60+</option>
            </select>
          </div>
          <div style={{ border: `1.5px solid ${COLORS.border}`, borderRadius: 12, overflow: "hidden", marginBottom: 10 }}>
            <select
              value={googleGender}
              onChange={e => setGoogleGender(e.target.value)}
              style={{ width: "100%", padding: "14px 16px", border: "none", outline: "none", fontSize: 15, fontFamily: "'DM Sans', sans-serif", color: COLORS.text, background: "rgba(255,255,255,0.02)", appearance: "none" }}
            >
              <option value="">Gender (optional)</option>
              <option value="MALE">Male</option>
              <option value="FEMALE">Female</option>
              <option value="OTHER">Other</option>
            </select>
          </div>
          <div style={{ marginBottom: 16 }}>
            <div style={{ fontSize: 12, color: COLORS.textDim, marginBottom: 6 }}>Birth date (optional)</div>
            <div style={{ border: `1.5px solid ${COLORS.border}`, borderRadius: 12, overflow: "hidden" }}>
              <input
                type="text"
                inputMode="numeric"
                aria-label="Birth date"
                placeholder="YYYY-MM-DD"
                value={googleBirthDate}
                onChange={e => setGoogleBirthDate(e.target.value)}
                style={{ width: "100%", padding: "14px 16px", border: "none", outline: "none", fontSize: 15, fontFamily: "'DM Sans', sans-serif", color: COLORS.text, background: "rgba(255,255,255,0.02)" }}
              />
            </div>
          </div>

          <button type="submit" disabled={loading} style={{
            width: "100%", padding: "14px 0", borderRadius: 12,
            background: `linear-gradient(135deg, ${COLORS.accent}, #5B21B6)`,
            border: "none", color: "white", cursor: loading ? "not-allowed" : "pointer",
            fontSize: 15, fontWeight: 600, fontFamily: "'DM Sans', sans-serif",
            opacity: loading ? 0.7 : 1,
          }}>
            {loading ? "Completing..." : "Complete Google Sign-in"}
          </button>
        </form>

        <div style={{ textAlign: "center", marginTop: 14 }}>
          <span onClick={() => { setGooglePending(null); setError(""); }} style={{ fontSize: 13, color: COLORS.accentLight, cursor: "pointer", fontWeight: 600 }}>
            Back
          </span>
        </div>
      </AuthModal>
    );
  }

  return (
    <AuthModal onClose={onClose}>
      {/* Title */}
      <div style={{ textAlign: "center", marginBottom: 28 }}>
        <div style={{ fontSize: 22, fontWeight: 700, color: COLORS.text, fontFamily: "'Outfit', sans-serif" }}>
          Welcome to PRE<span style={{ color: COLORS.accentLight }}>(D)</span>ATA
        </div>
      </div>

      {/* Google button */}
      <button onClick={async () => {
        setError(""); setLoading(true);
        try {
          const googleToken = await googleSignIn();
          if (typeof googleToken !== "string") throw new Error("Invalid Google token");
          const res = await authApi.googleLogin(googleToken);
          if (res?.needsAdditionalInfo) {
            const payload = decodeJwtPayload(googleToken);
            const googleId = res.googleId || payload.sub;
            const googleEmail = res.email || payload.email;
            if (!googleId || !googleEmail) {
              throw new Error("Missing Google registration information.");
            }
            setGooglePending({ googleId, email: googleEmail });
            setError("");
          } else {
            setToken(res.token ?? null);
            onClose();
          }
        } catch (err: unknown) {
          setError(err instanceof Error ? err.message : "Google login failed.");
        } finally {
          setLoading(false);
        }
      }} disabled={loading} style={{
        width: "100%", padding: "14px 0", borderRadius: 12,
        background: `linear-gradient(135deg, ${COLORS.accent}, #5B21B6)`,
        border: "none", color: "white", cursor: "pointer",
        fontSize: 15, fontWeight: 600, fontFamily: "'DM Sans', sans-serif",
        display: "flex", alignItems: "center", justifyContent: "center", gap: 10,
        transition: "all 0.15s",
        boxShadow: "0 4px 16px rgba(124,58,237,0.3)",
      }}
      onMouseEnter={e => { e.currentTarget.style.transform = "translateY(-1px)"; e.currentTarget.style.boxShadow = "0 6px 24px rgba(124,58,237,0.4)"; }}
      onMouseLeave={e => { e.currentTarget.style.transform = "translateY(0)"; e.currentTarget.style.boxShadow = "0 4px 16px rgba(124,58,237,0.3)"; }}
      >
        <svg width="18" height="18" viewBox="0 0 24 24">
          <path d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92a5.06 5.06 0 01-2.2 3.32v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.1z" fill="rgba(255,255,255,0.85)"/>
          <path d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z" fill="rgba(255,255,255,0.9)"/>
          <path d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z" fill="rgba(255,255,255,0.8)"/>
          <path d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z" fill="white"/>
        </svg>
        Log in with Google
      </button>

      {/* MetaMask button */}
      <button onClick={async () => {
        setError("");
        if (!window.ethereum) {
          setError("MetaMask is not installed. Please install it first.");
          return;
        }
        setLoading(true);
        try {
          const accounts = await window.ethereum.request({ method: "eth_requestAccounts" });
          const address = accounts[0];
          const noncePayload = await authApi.walletNonce(address);
          const nonce = typeof noncePayload?.nonce === "string" ? noncePayload.nonce : "";
          const message = typeof noncePayload?.message === "string" ? noncePayload.message : "";
          if (!nonce || !message) throw new Error("Failed to prepare wallet login challenge.");

          const signature = await window.ethereum.request({
            method: "personal_sign",
            params: [message, address],
          });
          const res = await authApi.walletLogin({ walletAddress: address, nonce, message, signature });
          setToken(res.token ?? null);
          onClose();
        } catch (err: unknown) {
          setError(err instanceof Error ? err.message : "MetaMask connection failed.");
        } finally {
          setLoading(false);
        }
      }} disabled={loading} style={{
        width: "100%", padding: "14px 0", borderRadius: 12, marginTop: 10,
        background: "rgba(255,255,255,0.03)",
        border: `1.5px solid ${COLORS.border}`, color: COLORS.text, cursor: "pointer",
        fontSize: 15, fontWeight: 600, fontFamily: "'DM Sans', sans-serif",
        display: "flex", alignItems: "center", justifyContent: "center", gap: 10,
        transition: "all 0.15s",
      }}
      onMouseEnter={e => { e.currentTarget.style.background = "rgba(255,255,255,0.06)"; e.currentTarget.style.borderColor = "rgba(255,255,255,0.12)"; }}
      onMouseLeave={e => { e.currentTarget.style.background = "rgba(255,255,255,0.03)"; e.currentTarget.style.borderColor = COLORS.border; }}
      >
        <svg width="20" height="20" viewBox="0 0 35 33" fill="none">
          <path d="M32.96 1l-13.14 9.72 2.45-5.73L32.96 1z" fill="#E17726" stroke="#E17726" strokeWidth=".25" strokeLinecap="round" strokeLinejoin="round"/>
          <path d="M2.66 1l13.02 9.81L13.35 4.99 2.66 1zM28.23 23.53l-3.5 5.34 7.49 2.06 2.15-7.28-6.14-.12zM.98 23.65l2.13 7.28 7.47-2.06-3.48-5.34-6.12.12z" fill="#E27625" stroke="#E27625" strokeWidth=".25" strokeLinecap="round" strokeLinejoin="round"/>
          <path d="M10.24 14.51l-2.1 3.16 7.46.34-.26-8.02-5.1 4.52zM25.39 14.51l-5.17-4.6-.17 8.1 7.46-.34-2.12-3.16zM10.58 28.87l4.49-2.16-3.88-3.02-.61 5.18zM20.55 26.71l4.48 2.16-.6-5.18-3.88 3.02z" fill="#E27625" stroke="#E27625" strokeWidth=".25" strokeLinecap="round" strokeLinejoin="round"/>
          <path d="M25.03 28.87l-4.48-2.16.36 2.93-.04 1.23 4.16-2zM10.58 28.87l4.16 2-.03-1.23.34-2.93-4.47 2.16z" fill="#D5BFB2" stroke="#D5BFB2" strokeWidth=".25" strokeLinecap="round" strokeLinejoin="round"/>
          <path d="M14.82 21.86l-3.74-1.1 2.64-1.22 1.1 2.32zM20.8 21.86l1.1-2.32 2.65 1.22-3.75 1.1z" fill="#233447" stroke="#233447" strokeWidth=".25" strokeLinecap="round" strokeLinejoin="round"/>
          <path d="M10.58 28.87l.63-5.34-4.11.12 3.48 5.22zM24.4 23.53l.63 5.34 3.5-5.22-4.13-.12zM27.51 17.67l-7.46.34.7 3.85 1.1-2.32 2.65 1.22 3.01-3.09zM11.08 20.76l2.64-1.22 1.1 2.32.7-3.85-7.46-.34 2.02 3.09z" fill="#CC6228" stroke="#CC6228" strokeWidth=".25" strokeLinecap="round" strokeLinejoin="round"/>
          <path d="M8.06 17.67l3.13 6.1-.1-3.01-3.03-3.09zM24.5 20.76l-.12 3.01 3.13-6.1-3.01 3.09zM15.52 18.01l-.7 3.85.87 4.5.2-5.93-.37-2.42zM20.05 18.01l-.36 2.41.18 5.94.87-4.5-.69-3.85z" fill="#E27525" stroke="#E27525" strokeWidth=".25" strokeLinecap="round" strokeLinejoin="round"/>
          <path d="M20.75 21.86l-.87 4.5.63.44 3.88-3.02.12-3.01-3.76 1.09zM11.08 20.76l.1 3.01 3.88 3.03.63-.44-.87-4.5-3.74-1.1z" fill="#F5841F" stroke="#F5841F" strokeWidth=".25" strokeLinecap="round" strokeLinejoin="round"/>
          <path d="M20.8 30.87l.03-1.23-.34-.29h-5.35l-.32.29.03 1.23-4.16-2 1.46 1.19 2.95 2.04h5.44l2.96-2.04 1.46-1.19-4.16 2z" fill="#C0AC9D" stroke="#C0AC9D" strokeWidth=".25" strokeLinecap="round" strokeLinejoin="round"/>
          <path d="M20.55 26.71l-.63-.44h-4.21l-.63.44-.34 2.93.32-.29h5.35l.34.29-.2-2.93z" fill="#161616" stroke="#161616" strokeWidth=".25" strokeLinecap="round" strokeLinejoin="round"/>
          <path d="M33.52 11.35l1.1-5.36L32.96 1l-12.4 9.19 4.77 4.03 6.75 1.97 1.49-1.74-.65-.47 1.03-.94-.79-.61 1.03-.79-.68-.51zM.97 5.99l1.12 5.36-.72.53 1.03.79-.78.61 1.03.94-.65.47 1.49 1.74 6.74-1.97 4.78-4.03L2.6 1 .97 5.99z" fill="#763E1A" stroke="#763E1A" strokeWidth=".25" strokeLinecap="round" strokeLinejoin="round"/>
          <path d="M32.08 16.23l-6.75-1.97 2.04 3.09-3.13 6.1 4.13-.05h6.16l-2.45-7.17zM10.24 14.26l-6.75 1.97-2.24 7.17h6.12l4.13.05-3.13-6.1 1.87-3.09zM20.05 18.01l.43-7.49 1.96-5.28h-8.7l1.93 5.28.45 7.49.17 2.44.01 5.91h4.22l.02-5.91.51-2.44z" fill="#F5841F" stroke="#F5841F" strokeWidth=".25" strokeLinecap="round" strokeLinejoin="round"/>
        </svg>
        Log in with MetaMask
      </button>

      {/* Divider */}
      <div style={{ display: "flex", alignItems: "center", gap: 14, margin: "24px 0" }}>
        <div style={{ flex: 1, height: 1, background: COLORS.border }} />
        <span style={{ fontSize: 13, color: COLORS.textDim, fontWeight: 500 }}>or</span>
        <div style={{ flex: 1, height: 1, background: COLORS.border }} />
      </div>

      {error && (
        <div style={{ padding: "10px 14px", borderRadius: 10, marginBottom: 14, background: "rgba(239,68,68,0.08)", border: "1px solid rgba(239,68,68,0.12)", fontSize: 13, color: COLORS.red, fontWeight: 500 }}>{error}</div>
      )}

      {/* Email + Password form */}
      <form onSubmit={handleLogin}>
        <div style={{
          border: `1.5px solid ${COLORS.border}`, borderRadius: 12, overflow: "hidden", marginBottom: 12,
        }}>
          <input type="email" placeholder="Email address"
            value={email} onChange={e => setEmail(e.target.value)}
            style={{
              width: "100%", padding: "14px 16px", border: "none", outline: "none",
              fontSize: 15, fontFamily: "'DM Sans', sans-serif",
              color: COLORS.text, background: "rgba(255,255,255,0.02)",
            }}
          />
        </div>
        <div style={{
          border: `1.5px solid ${COLORS.border}`, borderRadius: 12, overflow: "hidden", marginBottom: 6,
        }}>
          <input type="password" placeholder="Password"
            value={password} onChange={e => setPassword(e.target.value)}
            style={{
              width: "100%", padding: "14px 16px", border: "none", outline: "none",
              fontSize: 15, fontFamily: "'DM Sans', sans-serif",
              color: COLORS.text, background: "rgba(255,255,255,0.02)",
            }}
          />
        </div>
        <div style={{ textAlign: "right", marginBottom: 16 }}>
          <a href="#" style={{ fontSize: 12, color: COLORS.accentLight, textDecoration: "none", fontWeight: 500 }}>Forgot password?</a>
        </div>
        <button type="submit" disabled={loading} style={{
          width: "100%", padding: "14px 0", borderRadius: 12,
          background: `linear-gradient(135deg, ${COLORS.accent}, #5B21B6)`,
          border: "none", color: "white", cursor: loading ? "not-allowed" : "pointer",
          fontSize: 15, fontWeight: 600, fontFamily: "'DM Sans', sans-serif",
          opacity: loading ? 0.7 : 1, transition: "all 0.15s",
          boxShadow: "0 4px 16px rgba(124,58,237,0.3)",
          display: "flex", alignItems: "center", justifyContent: "center", gap: 8,
        }}>
          {loading && <div style={{ width: 16, height: 16, border: "2px solid rgba(255,255,255,0.3)", borderTopColor: "white", borderRadius: "50%", animation: "spin 0.6s linear infinite" }} />}
          {loading ? "Signing in..." : "Sign In"}
        </button>
      </form>

      {/* Sign up link */}
      <div style={{ textAlign: "center", marginTop: 24, fontSize: 14, color: COLORS.textDim }}>
        Don't have an account?{" "}
        <span onClick={onSwitchToSignup} style={{ color: COLORS.accentLight, fontWeight: 600, cursor: "pointer" }}>Sign Up</span>
      </div>

      {/* Terms */}
      <div style={{ textAlign: "center", marginTop: 16, fontSize: 12, color: COLORS.textDim }}>
        <a href="#" style={{ color: COLORS.textMuted, textDecoration: "none" }}>Terms</a>
        {" · "}
        <a href="#" style={{ color: COLORS.textMuted, textDecoration: "none" }}>Privacy Policy</a>
      </div>
    </AuthModal>
  );
}

function SignupModal({ onClose, onSwitchToLogin }) {
  const [email, setEmail] = useState("");
  const [step, setStep] = useState("initial"); // initial | verify | form
  const [name, setName] = useState("");
  const [password, setPassword] = useState("");
  const [passwordConfirm, setPasswordConfirm] = useState("");
  const [verifyCode, setVerifyCode] = useState("");
  const [error, setError] = useState("");
  const [infoMessage, setInfoMessage] = useState("");
  const [demoCode, setDemoCode] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [googlePending, setGooglePending] = useState<{ googleId: string; email: string } | null>(null);
  const [googleCountryCode, setGoogleCountryCode] = useState("KR");
  const [googleJobCategory, setGoogleJobCategory] = useState("");
  const [googleAgeGroup, setGoogleAgeGroup] = useState("");
  const [googleGender, setGoogleGender] = useState("");
  const [googleBirthDate, setGoogleBirthDate] = useState("");

  const handleContinue = async () => {
    if (!email) { setError("Please enter your email."); return; }
    setError(""); setInfoMessage(""); setDemoCode(null); setLoading(true);
    try {
      const res = await authApi.sendCode(email) as Record<string, unknown>;
      if (typeof res?.message === "string") setInfoMessage(res.message);
      if (typeof res?.debugCode === "string") setDemoCode(res.debugCode);
      setStep("verify");
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : "Failed to send code.");
    } finally {
      setLoading(false);
    }
  };

  const handleVerify = async () => {
    if (!verifyCode) { setError("Please enter the verification code."); return; }
    setError(""); setInfoMessage(""); setLoading(true);
    try {
      await authApi.verifyCode(email, verifyCode);
      setStep("form");
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : "Invalid code.");
    } finally {
      setLoading(false);
    }
  };

  // FIX #3: Backend requires email, code, password, passwordConfirm
  const handleSignup = async (e) => {
    e.preventDefault();
    if (!name || !password) { setError("Please fill in all fields."); return; }
    if (password.length < 8) { setError("Password must be at least 8 characters."); return; }
    if (password !== passwordConfirm) { setError("Passwords do not match."); return; }
    setError(""); setLoading(true);
    try {
      const res = await authApi.completeSignup({
        email,
        code: verifyCode,
        password,
        passwordConfirm,
        nickname: name,
      });
      setToken(res.token ?? null);
      onClose();
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : "Signup failed.");
    } finally {
      setLoading(false);
    }
  };

  const handleCompleteGoogleRegistration = async (e) => {
    e.preventDefault();
    if (!googlePending) return;
    if (!googleCountryCode.trim()) {
      setError("Country code is required.");
      return;
    }
    setError("");
    setLoading(true);
    try {
      const res = await authApi.googleCompleteRegistration({
        googleId: googlePending.googleId,
        email: googlePending.email,
        countryCode: googleCountryCode.trim().toUpperCase(),
        jobCategory: googleJobCategory.trim() || undefined,
        ageGroup: googleAgeGroup ? Number(googleAgeGroup) : undefined,
        gender: googleGender || undefined,
        birthDate: googleBirthDate || undefined,
      });
      setToken(res.token ?? null);
      onClose();
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : "Google registration failed.");
    } finally {
      setLoading(false);
    }
  };

  if (googlePending) {
    return (
      <AuthModal onClose={onClose}>
        <div style={{ textAlign: "center", marginBottom: 20 }}>
          <div style={{ fontSize: 22, fontWeight: 700, color: COLORS.text, fontFamily: "'Outfit', sans-serif" }}>
            Complete Google Sign-up
          </div>
          <div style={{ fontSize: 13, color: COLORS.textDim, marginTop: 8 }}>{googlePending.email}</div>
        </div>

        {error && (
          <div style={{ padding: "10px 14px", borderRadius: 10, marginBottom: 14, background: "rgba(239,68,68,0.08)", border: "1px solid rgba(239,68,68,0.12)", fontSize: 13, color: COLORS.red, fontWeight: 500 }}>{error}</div>
        )}

        <form onSubmit={handleCompleteGoogleRegistration}>
          <div style={{ border: `1.5px solid ${COLORS.border}`, borderRadius: 12, overflow: "hidden", marginBottom: 10 }}>
            <select
              value={googleCountryCode}
              onChange={e => setGoogleCountryCode(e.target.value)}
              style={{ width: "100%", padding: "14px 16px", border: "none", outline: "none", fontSize: 15, fontFamily: "'DM Sans', sans-serif", color: COLORS.text, background: "rgba(255,255,255,0.02)", appearance: "none" }}
            >
              <option value="KR">South Korea (KR)</option>
              <option value="US">United States (US)</option>
              <option value="JP">Japan (JP)</option>
              <option value="CN">China (CN)</option>
              <option value="GB">United Kingdom (GB)</option>
              <option value="DE">Germany (DE)</option>
              <option value="FR">France (FR)</option>
              <option value="SG">Singapore (SG)</option>
            </select>
          </div>
          <div style={{ border: `1.5px solid ${COLORS.border}`, borderRadius: 12, overflow: "hidden", marginBottom: 10 }}>
            <select
              value={googleJobCategory}
              onChange={e => setGoogleJobCategory(e.target.value)}
              style={{ width: "100%", padding: "14px 16px", border: "none", outline: "none", fontSize: 15, fontFamily: "'DM Sans', sans-serif", color: COLORS.text, background: "rgba(255,255,255,0.02)", appearance: "none" }}
            >
              <option value="">Job category (optional)</option>
              <option value="Student">Student</option>
              <option value="IT">IT</option>
              <option value="Finance">Finance</option>
              <option value="Medical">Medical</option>
              <option value="Other">Other</option>
            </select>
          </div>
          <div style={{ border: `1.5px solid ${COLORS.border}`, borderRadius: 12, overflow: "hidden", marginBottom: 10 }}>
            <select
              value={googleAgeGroup}
              onChange={e => setGoogleAgeGroup(e.target.value)}
              style={{ width: "100%", padding: "14px 16px", border: "none", outline: "none", fontSize: 15, fontFamily: "'DM Sans', sans-serif", color: COLORS.text, background: "rgba(255,255,255,0.02)", appearance: "none" }}
            >
              <option value="">Age group (optional)</option>
              <option value="10">10s</option>
              <option value="20">20s</option>
              <option value="30">30s</option>
              <option value="40">40s</option>
              <option value="50">50s</option>
              <option value="60">60+</option>
            </select>
          </div>
          <div style={{ border: `1.5px solid ${COLORS.border}`, borderRadius: 12, overflow: "hidden", marginBottom: 10 }}>
            <select
              value={googleGender}
              onChange={e => setGoogleGender(e.target.value)}
              style={{ width: "100%", padding: "14px 16px", border: "none", outline: "none", fontSize: 15, fontFamily: "'DM Sans', sans-serif", color: COLORS.text, background: "rgba(255,255,255,0.02)", appearance: "none" }}
            >
              <option value="">Gender (optional)</option>
              <option value="MALE">Male</option>
              <option value="FEMALE">Female</option>
              <option value="OTHER">Other</option>
            </select>
          </div>
          <div style={{ marginBottom: 16 }}>
            <div style={{ fontSize: 12, color: COLORS.textDim, marginBottom: 6 }}>Birth date (optional)</div>
            <div style={{ border: `1.5px solid ${COLORS.border}`, borderRadius: 12, overflow: "hidden" }}>
              <input
                type="text"
                inputMode="numeric"
                aria-label="Birth date"
                placeholder="YYYY-MM-DD"
                value={googleBirthDate}
                onChange={e => setGoogleBirthDate(e.target.value)}
                style={{ width: "100%", padding: "14px 16px", border: "none", outline: "none", fontSize: 15, fontFamily: "'DM Sans', sans-serif", color: COLORS.text, background: "rgba(255,255,255,0.02)" }}
              />
            </div>
          </div>

          <button type="submit" disabled={loading} style={{
            width: "100%", padding: "14px 0", borderRadius: 12,
            background: `linear-gradient(135deg, ${COLORS.accent}, #5B21B6)`,
            border: "none", color: "white", cursor: loading ? "not-allowed" : "pointer",
            fontSize: 15, fontWeight: 600, fontFamily: "'DM Sans', sans-serif",
            opacity: loading ? 0.7 : 1,
          }}>
            {loading ? "Completing..." : "Complete Google Sign-up"}
          </button>
        </form>

        <div style={{ textAlign: "center", marginTop: 14 }}>
          <span onClick={() => { setGooglePending(null); setError(""); }} style={{ fontSize: 13, color: COLORS.accentLight, cursor: "pointer", fontWeight: 600 }}>
            Back
          </span>
        </div>
      </AuthModal>
    );
  }

  if (step === "form") {
    return (
      <AuthModal onClose={onClose}>
        <div style={{ textAlign: "center", marginBottom: 28 }}>
          <div style={{ fontSize: 22, fontWeight: 700, color: COLORS.text, fontFamily: "'Outfit', sans-serif" }}>
            Complete your profile
          </div>
          <div style={{ fontSize: 14, color: COLORS.textDim, marginTop: 8 }}>{email}</div>
        </div>

        {error && (
          <div style={{ padding: "10px 14px", borderRadius: 10, marginBottom: 16, background: "rgba(239,68,68,0.08)", border: "1px solid rgba(239,68,68,0.12)", fontSize: 13, color: COLORS.red, fontWeight: 500 }}>{error}</div>
        )}

        <form onSubmit={handleSignup}>
          <div style={{ border: `1.5px solid ${COLORS.border}`, borderRadius: 12, overflow: "hidden", marginBottom: 12 }}>
            <input type="text" placeholder="Display name" autoFocus
              value={name} onChange={e => setName(e.target.value)}
              style={{ width: "100%", padding: "14px 16px", border: "none", outline: "none", fontSize: 15, fontFamily: "'DM Sans', sans-serif", color: COLORS.text, background: "rgba(255,255,255,0.02)" }}
            />
          </div>
          <div style={{ border: `1.5px solid ${COLORS.border}`, borderRadius: 12, overflow: "hidden", marginBottom: 12 }}>
            <input type="password" placeholder="Password (min. 8 characters)"
              value={password} onChange={e => setPassword(e.target.value)}
              style={{ width: "100%", padding: "14px 16px", border: "none", outline: "none", fontSize: 15, fontFamily: "'DM Sans', sans-serif", color: COLORS.text, background: "rgba(255,255,255,0.02)" }}
            />
          </div>
          <div style={{ border: `1.5px solid ${COLORS.border}`, borderRadius: 12, overflow: "hidden", marginBottom: 20 }}>
            <input type="password" placeholder="Confirm password"
              value={passwordConfirm} onChange={e => setPasswordConfirm(e.target.value)}
              style={{ width: "100%", padding: "14px 16px", border: "none", outline: "none", fontSize: 15, fontFamily: "'DM Sans', sans-serif", color: COLORS.text, background: "rgba(255,255,255,0.02)" }}
            />
          </div>
          <button type="submit" disabled={loading} style={{
            width: "100%", padding: "14px 0", borderRadius: 12,
            background: `linear-gradient(135deg, ${COLORS.accent}, #5B21B6)`,
            border: "none", color: "white", cursor: loading ? "not-allowed" : "pointer",
            fontSize: 15, fontWeight: 600, fontFamily: "'DM Sans', sans-serif",
            opacity: loading ? 0.7 : 1, transition: "all 0.15s",
            boxShadow: "0 4px 16px rgba(124,58,237,0.3)",
            display: "flex", alignItems: "center", justifyContent: "center", gap: 8,
          }}>
            {loading && <div style={{ width: 16, height: 16, border: "2px solid rgba(255,255,255,0.3)", borderTopColor: "white", borderRadius: "50%", animation: "spin 0.6s linear infinite" }} />}
            {loading ? "Creating account..." : "Create Account"}
          </button>
        </form>

        <div style={{ display: "flex", justifyContent: "center", marginTop: 18 }}>
          <span onClick={() => { setStep("initial"); setError(""); }} style={{ fontSize: 13, color: COLORS.accentLight, fontWeight: 500, cursor: "pointer" }}>Back</span>
        </div>
      </AuthModal>
    );
  }

  if (step === "verify") {
    return (
      <AuthModal onClose={onClose}>
        <div style={{ textAlign: "center", marginBottom: 28 }}>
          <div style={{ fontSize: 22, fontWeight: 700, color: COLORS.text, fontFamily: "'Outfit', sans-serif" }}>
            Verify your email
          </div>
          <div style={{ fontSize: 14, color: COLORS.textDim, marginTop: 8 }}>{email}</div>
        </div>

        {error && (
          <div style={{ padding: "10px 14px", borderRadius: 10, marginBottom: 14, background: "rgba(239,68,68,0.08)", border: "1px solid rgba(239,68,68,0.12)", fontSize: 13, color: COLORS.red, fontWeight: 500 }}>{error}</div>
        )}

        {infoMessage && (
          <div style={{ padding: "10px 14px", borderRadius: 10, marginBottom: 12, background: "rgba(124,58,237,0.12)", border: "1px solid rgba(124,58,237,0.25)", fontSize: 13, color: COLORS.accentLight, fontWeight: 500 }}>
            {infoMessage}
          </div>
        )}
        {demoCode && (
          <div style={{ padding: "10px 14px", borderRadius: 10, marginBottom: 14, background: "rgba(34,197,94,0.08)", border: "1px solid rgba(34,197,94,0.2)", fontSize: 13, color: COLORS.green, fontWeight: 600, fontFamily: "'JetBrains Mono', monospace" }}>
            Demo verification code: {demoCode}
          </div>
        )}

        <div style={{ border: `1.5px solid ${COLORS.border}`, borderRadius: 12, overflow: "hidden", marginBottom: 14 }}>
          <input
            type="text"
            placeholder="Enter verification code"
            value={verifyCode}
            onChange={e => setVerifyCode(e.target.value)}
            onKeyDown={e => { if (e.key === "Enter") handleVerify(); }}
            style={{ width: "100%", padding: "14px 16px", border: "none", outline: "none", fontSize: 15, fontFamily: "'DM Sans', sans-serif", color: COLORS.text, background: "rgba(255,255,255,0.02)" }}
          />
        </div>

        <button
          onClick={handleVerify}
          disabled={loading}
          style={{
            width: "100%", padding: "14px 0", borderRadius: 12,
            background: `linear-gradient(135deg, ${COLORS.accent}, #5B21B6)`,
            border: "none", color: "white", cursor: loading ? "not-allowed" : "pointer",
            fontSize: 15, fontWeight: 600, fontFamily: "'DM Sans', sans-serif",
            opacity: loading ? 0.7 : 1, transition: "all 0.15s",
            boxShadow: "0 4px 16px rgba(124,58,237,0.3)",
          }}
        >
          {loading ? "Verifying..." : "Verify Code"}
        </button>

        <div style={{ display: "flex", justifyContent: "space-between", marginTop: 14 }}>
          <span onClick={() => { setStep("initial"); setError(""); }} style={{ fontSize: 13, color: COLORS.accentLight, cursor: "pointer", fontWeight: 600 }}>
            Back
          </span>
          <span onClick={handleContinue} style={{ fontSize: 13, color: COLORS.accentLight, cursor: "pointer", fontWeight: 600 }}>
            Resend code
          </span>
        </div>
      </AuthModal>
    );
  }

  return (
    <AuthModal onClose={onClose}>
      <div style={{ textAlign: "center", marginBottom: 28 }}>
        <div style={{ fontSize: 22, fontWeight: 700, color: COLORS.text, fontFamily: "'Outfit', sans-serif" }}>
          Join PRE<span style={{ color: COLORS.accentLight }}>(D)</span>ATA
        </div>
      </div>

      {/* Google */}
      <button onClick={async () => {
        setError(""); setLoading(true);
        try {
          const googleToken = await googleSignIn();
          if (typeof googleToken !== "string") throw new Error("Invalid Google token");
          const res = await authApi.googleLogin(googleToken);
          if (res?.needsAdditionalInfo) {
            const payload = decodeJwtPayload(googleToken);
            const googleId = res.googleId || payload.sub;
            const googleEmail = res.email || payload.email;
            if (!googleId || !googleEmail) {
              throw new Error("Missing Google registration information.");
            }
            setGooglePending({ googleId, email: googleEmail });
            setError("");
          } else {
            setToken(res.token ?? null);
            onClose();
          }
        } catch (err: unknown) {
          setError(err instanceof Error ? err.message : "Google sign-up failed.");
        } finally {
          setLoading(false);
        }
      }} disabled={loading} style={{
        width: "100%", padding: "14px 0", borderRadius: 12,
        background: `linear-gradient(135deg, ${COLORS.accent}, #5B21B6)`,
        border: "none", color: "white", cursor: "pointer",
        fontSize: 15, fontWeight: 600, fontFamily: "'DM Sans', sans-serif",
        display: "flex", alignItems: "center", justifyContent: "center", gap: 10,
        transition: "all 0.15s",
        boxShadow: "0 4px 16px rgba(124,58,237,0.3)",
      }}
      onMouseEnter={e => { e.currentTarget.style.transform = "translateY(-1px)"; e.currentTarget.style.boxShadow = "0 6px 24px rgba(124,58,237,0.4)"; }}
      onMouseLeave={e => { e.currentTarget.style.transform = "translateY(0)"; e.currentTarget.style.boxShadow = "0 4px 16px rgba(124,58,237,0.3)"; }}
      >
        <svg width="18" height="18" viewBox="0 0 24 24">
          <path d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92a5.06 5.06 0 01-2.2 3.32v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.1z" fill="rgba(255,255,255,0.85)"/>
          <path d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z" fill="rgba(255,255,255,0.9)"/>
          <path d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z" fill="rgba(255,255,255,0.8)"/>
          <path d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z" fill="white"/>
        </svg>
        Sign up with Google
      </button>

      <div style={{ display: "flex", alignItems: "center", gap: 14, margin: "24px 0" }}>
        <div style={{ flex: 1, height: 1, background: COLORS.border }} />
        <span style={{ fontSize: 13, color: COLORS.textDim, fontWeight: 500 }}>or</span>
        <div style={{ flex: 1, height: 1, background: COLORS.border }} />
      </div>

      {error && (
        <div style={{ padding: "10px 14px", borderRadius: 10, marginBottom: 14, background: "rgba(239,68,68,0.08)", border: "1px solid rgba(239,68,68,0.12)", fontSize: 13, color: COLORS.red, fontWeight: 500 }}>{error}</div>
      )}

      {/* Email + Continue */}
      <div style={{
        display: "flex", alignItems: "center",
        border: `1.5px solid ${COLORS.border}`, borderRadius: 12,
        overflow: "hidden",
      }}>
        <input
          type="email" placeholder="Email address"
          value={email} onChange={e => setEmail(e.target.value)}
          onKeyDown={e => { if (e.key === "Enter") handleContinue(); }}
          style={{
            flex: 1, padding: "14px 16px", border: "none", outline: "none",
            fontSize: 15, fontFamily: "'DM Sans', sans-serif",
            color: COLORS.text, background: "transparent",
          }}
        />
        <button onClick={handleContinue} disabled={loading} style={{
          padding: "14px 24px", border: "none", cursor: loading ? "not-allowed" : "pointer",
          background: `linear-gradient(135deg, ${COLORS.accent}, #5B21B6)`, color: "white",
          fontSize: 14, fontWeight: 700, fontFamily: "'DM Sans', sans-serif",
          borderLeft: `1.5px solid rgba(255,255,255,0.12)`,
          transition: "all 0.15s", whiteSpace: "nowrap", opacity: loading ? 0.7 : 1,
          boxShadow: "0 4px 16px rgba(124,58,237,0.3)",
        }}
        onMouseEnter={e => { if (!loading) (e.currentTarget as HTMLElement).style.boxShadow = "0 6px 24px rgba(124,58,237,0.45)"; }}
        onMouseLeave={e => { (e.currentTarget as HTMLElement).style.boxShadow = "0 4px 16px rgba(124,58,237,0.3)"; }}
        >{loading ? "Sending..." : "Continue"}</button>
      </div>

      <div style={{ textAlign: "center", marginTop: 24, fontSize: 14, color: COLORS.textDim }}>
        Already have an account?{" "}
        <span onClick={onSwitchToLogin} style={{ color: COLORS.accentLight, fontWeight: 600, cursor: "pointer" }}>Sign In</span>
      </div>

      <div style={{ textAlign: "center", marginTop: 16, fontSize: 12, color: COLORS.textDim }}>
        <a href="#" style={{ color: COLORS.textMuted, textDecoration: "none" }}>Terms</a>
        {" · "}
        <a href="#" style={{ color: COLORS.textMuted, textDecoration: "none" }}>Privacy Policy</a>
      </div>
    </AuthModal>
  );
}

export { AuthModal, LoginModal, SignupModal };

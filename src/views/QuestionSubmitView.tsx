import React, { useState } from "react";
import { COLORS } from "../theme";
import { EXPLORE_CATEGORIES } from "../data/exploreData";
import { useQuestionSubmit } from "../hooks/useApi";
import MarketCard from "../components/MarketCard";

type QuestionSubmitViewProps = {
  onBack?: () => void;
};

function QuestionSubmitView({ onBack = () => {} }: QuestionSubmitViewProps = {}) {
  const PLATFORM_FEE_PERCENT = 20;
  const DISTRIBUTABLE_FEE_PERCENT = 100 - PLATFORM_FEE_PERCENT;
  const [step, setStep] = useState(1); // 1: form, 2: preview, 3: done
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [category, setCategory] = useState("");
  const [subCategory, setSubCategory] = useState("");
  const [resolutionType, setResolutionType] = useState("auto"); // auto | manual
  const [resolutionSource, setResolutionSource] = useState("");
  const [resolutionCriteria, setResolutionCriteria] = useState("");
  const [tags, setTags] = useState<string[]>([]);
  const [tagInput, setTagInput] = useState("");
  const [boost, setBoost] = useState(false);
  const [imagePreview, setImagePreview] = useState<string | null>(null);
  const [submitError, setSubmitError] = useState<string | null>(null);

  const { loading: submitting, createAndSubmit } = useQuestionSubmit();

  const submitCategories = EXPLORE_CATEGORIES.filter(c => c.id !== "trending" && c.id !== "live");

  const currentCat: any = submitCategories.find(c => c.id === category);

  const addTag = () => {
    const t = tagInput.trim().toLowerCase();
    if (t && tags.length < 5 && !tags.includes(t)) {
      setTags([...tags, t]);
      setTagInput("");
    }
  };

  const removeTag = (idx: number) => setTags(tags.filter((_, i) => i !== idx));

  const [voteWindowType, setVoteWindowType] = useState("D1"); // H6 | D1 | D3
  const [creatorSplit, setCreatorSplit] = useState(50); // 0-100, step 10
  const creatorFeePercent = Math.round(DISTRIBUTABLE_FEE_PERCENT * (creatorSplit / 100));
  const voterFeePercent = DISTRIBUTABLE_FEE_PERCENT - creatorFeePercent;
  const voteWindowLabel = voteWindowType === "H6" ? "6H" : voteWindowType === "D3" ? "3D" : "1D";

  const hasResolutionRule = resolutionType === "auto"
    ? resolutionSource.trim().length >= 5
    : resolutionCriteria.trim().length >= 5;
  const isValid = title.trim().length >= 10 && category && hasResolutionRule;

  const handleSubmit = async () => {
    if (!isValid) return;
    setSubmitError(null);
    try {
      // FIX #5: Send backend-compatible field names
      await createAndSubmit({
        title,
        description,
        category,
        voteWindowType, // H6 | D1 | D3 (not endDate)
        settlementMode: resolutionType === "auto" ? "OBJECTIVE_RULE" : "VOTE_RESULT",
        resolutionRule: resolutionType === "auto" ? resolutionSource : resolutionCriteria,
        resolutionSource,
        tags,
        creatorSplitInPool: creatorSplit,
      });
      setStep(3);
    } catch (err: any) {
      setSubmitError(err.message);
    }
  };

  // Mock market card for preview
  const previewMarket = {
    id: 999, title: title || "Your question will appear here",
    category: category || "tech", yesPrice: 0.50, volume: "0",
    voters: 0, comments: 0, change24h: 0, timeLeft: voteWindowLabel,
  };

  const sectionStyle: any = {
    background: COLORS.surface, border: `1px solid ${COLORS.border}`,
    borderRadius: 14, padding: 20, marginBottom: 16,
  };
  const labelStyle: any = { fontSize: 13, fontWeight: 600, color: COLORS.text, marginBottom: 8, display: "block" };
  const inputStyle: any = {
    width: "100%", padding: "10px 14px", borderRadius: 10,
    background: "rgba(255,255,255,0.02)", border: `1.5px solid ${COLORS.border}`,
    color: COLORS.text, fontSize: 14, fontFamily: "'DM Sans', sans-serif",
    outline: "none", transition: "border-color 0.15s", boxSizing: "border-box",
  };
  const hintStyle: any = { fontSize: 11, color: COLORS.textDim, marginTop: 4 };

  return (
    <div style={{ minHeight: "100vh", background: COLORS.bg }}>
      {/* Top bar */}
      <div className="mobile-only" style={{
        position: "sticky", top: 0, zIndex: 50,
        background: `${COLORS.bg}f2`, backdropFilter: "blur(12px)",
        borderBottom: `1px solid ${COLORS.border}`,
        padding: "12px 24px",
        display: "flex", alignItems: "center", justifyContent: "space-between",
        maxWidth: 1320, margin: "0 auto",
      }}>
        <button onClick={onBack} style={{
          background: "none", border: "none", cursor: "pointer",
          color: COLORS.textMuted, display: "flex", alignItems: "center", gap: 6,
          fontSize: 14, fontWeight: 500, fontFamily: "'DM Sans', sans-serif",
        }}
        onMouseEnter={e => e.currentTarget.style.color = COLORS.text}
        onMouseLeave={e => e.currentTarget.style.color = COLORS.textMuted}
        >
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M19 12H5"/><polyline points="12 19 5 12 12 5"/></svg>
          Back
        </button>
        {step === 1 && (
          <div style={{ display: "flex", gap: 8 }}>
            <span style={{ fontSize: 12, color: COLORS.textDim }}>Step 1 of 2</span>
          </div>
        )}
      </div>

      {/* ── Step 3: Done ── */}
      {step === 3 && (
        <div style={{ maxWidth: 560, margin: "0 auto", padding: "80px 24px", textAlign: "center" }}>
          <div style={{
            width: 80, height: 80, borderRadius: "50%", margin: "0 auto 20px",
            background: COLORS.greenBg, display: "flex", alignItems: "center", justifyContent: "center",
          }}>
            <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke={COLORS.green} strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round"><polyline points="20 6 9 17 4 12"/></svg>
          </div>
          <h2 style={{ fontSize: 24, fontWeight: 700, fontFamily: "'Outfit', sans-serif", marginBottom: 8 }}>Question Submitted!</h2>
          <p style={{ fontSize: 14, color: COLORS.textMuted, lineHeight: 1.6, marginBottom: 24 }}>
            Your question is now live for community voting.
            <br />Once it passes the vote threshold, it becomes an active betting market.
          </p>
          <div style={sectionStyle}>
            <MarketCard market={previewMarket} index={0} onBet={() => {}} />
          </div>
          <button onClick={onBack} style={{
            padding: "12px 32px", borderRadius: 10,
            background: `linear-gradient(135deg, ${COLORS.accent}, #5B21B6)`,
            border: "none", color: "white", cursor: "pointer",
            fontSize: 15, fontWeight: 600, fontFamily: "'DM Sans', sans-serif",
          }}>Back to Home</button>
        </div>
      )}

      {/* ── Step 2: Preview ── */}
      {step === 2 && (
        <div style={{ maxWidth: 700, margin: "0 auto", padding: "24px 24px" }}>
          <h2 style={{ fontSize: 22, fontWeight: 700, fontFamily: "'Outfit', sans-serif", marginBottom: 20 }}>Preview Your Question</h2>

          <div style={{ ...sectionStyle, marginBottom: 20 }}>
            <MarketCard market={previewMarket} index={0} onBet={() => {}} />
          </div>

          <div style={sectionStyle}>
            <div style={{ fontSize: 13, color: COLORS.textDim, marginBottom: 8 }}>Details</div>
            {[
              { label: "Category", value: `${category}${subCategory ? ` > ${subCategory}` : ""}` },
              { label: "Vote Window", value: voteWindowLabel },
              { label: "Resolution", value: resolutionType === "auto" ? `Auto: ${resolutionSource || "—"}` : `Manual: ${resolutionCriteria || "—"}` },
              { label: "Tags", value: tags.length > 0 ? tags.join(", ") : "—" },
            ].map((r, i) => (
              <div key={i} style={{ display: "flex", justifyContent: "space-between", padding: "6px 0", borderBottom: i < 3 ? `1px solid ${COLORS.border}` : "none" }}>
                <span style={{ fontSize: 12, color: COLORS.textDim }}>{r.label}</span>
                <span style={{ fontSize: 12, fontWeight: 500, color: COLORS.text, textAlign: "right", maxWidth: "60%", wordBreak: "break-all" }}>{r.value}</span>
              </div>
            ))}
          </div>

          {description && (
            <div style={sectionStyle}>
              <div style={{ fontSize: 13, color: COLORS.textDim, marginBottom: 6 }}>Description</div>
              <div style={{ fontSize: 13, color: COLORS.textMuted, lineHeight: 1.5 }}>{description}</div>
            </div>
          )}

          <div style={{ display: "flex", gap: 12 }}>
            <button onClick={() => setStep(1)} style={{
              flex: 1, padding: "14px 0", borderRadius: 10,
              background: "rgba(255,255,255,0.04)", border: `1px solid ${COLORS.border}`,
              color: COLORS.textMuted, cursor: "pointer",
              fontSize: 15, fontWeight: 600, fontFamily: "'DM Sans', sans-serif",
            }}>Edit</button>
            <button onClick={handleSubmit} style={{
              flex: 2, padding: "14px 0", borderRadius: 10,
              background: `linear-gradient(135deg, ${COLORS.accent}, #5B21B6)`,
              border: "none", color: "white", cursor: "pointer",
              fontSize: 15, fontWeight: 700, fontFamily: "'DM Sans', sans-serif",
              boxShadow: "0 4px 16px rgba(124,58,237,0.3)",
            }}>Submit Question</button>
          </div>
        </div>
      )}

      {/* ── Step 1: Form ── */}
      {step === 1 && (
        <div style={{ maxWidth: 700, margin: "0 auto", padding: "24px 24px" }}>
          <h2 style={{ fontSize: 22, fontWeight: 700, fontFamily: "'Outfit', sans-serif", marginBottom: 4 }}>Submit a Question</h2>
          <p style={{ fontSize: 13, color: COLORS.textDim, marginBottom: 24 }}>Create a prediction market question for the community to vote on and bet.</p>

          {/* Title */}
          <div style={sectionStyle}>
            <label style={labelStyle}>Question Title *</label>
            <input
              value={title} onChange={e => setTitle(e.target.value)}
              placeholder="Will Bitcoin hit $150K before July 2026?"
              maxLength={120}
              style={inputStyle}
            />
            <div style={{ ...hintStyle, display: "flex", justifyContent: "space-between" }}>
              <span>Must be a clear yes/no question</span>
              <span>{title.length}/120</span>
            </div>
          </div>

          {/* Description */}
          <div style={sectionStyle}>
            <label style={labelStyle}>Description <span style={{ color: COLORS.textDim, fontWeight: 400 }}>(optional)</span></label>
            <textarea
              value={description} onChange={e => setDescription(e.target.value)}
              placeholder="Provide background context, relevant data, or reasoning..."
              rows={3} maxLength={500}
              style={{ ...inputStyle, resize: "vertical", minHeight: 80 }}
            />
            <div style={{ ...hintStyle, textAlign: "right" }}>{description.length}/500</div>
          </div>

          {/* Category */}
          <div style={sectionStyle}>
            <label style={labelStyle}>Category *</label>
            <div style={{ display: "flex", flexWrap: "wrap", gap: 6, marginBottom: (currentCat?.subs?.length ?? 0) > 0 ? 12 : 0 }}>
              {submitCategories.map(cat => (
                <button key={cat.id} onClick={() => { setCategory(cat.id); setSubCategory(""); }} style={{
                  padding: "7px 14px", borderRadius: 8, border: "none", cursor: "pointer",
                  background: category === cat.id ? COLORS.accentGlow : "rgba(255,255,255,0.03)",
                  color: category === cat.id ? COLORS.accentLight : COLORS.textDim,
                  fontSize: 13, fontWeight: 500, fontFamily: "'DM Sans', sans-serif",
                  transition: "all 0.15s",
                }}>{cat.label}</button>
              ))}
            </div>
            {(currentCat?.subs?.length ?? 0) > 0 && (
              <div style={{ display: "flex", flexWrap: "wrap", gap: 4 }}>
                {(currentCat?.subs ?? []).map((sub: any) => (
                  <button key={sub.id} onClick={() => setSubCategory(sub.id)} style={{
                    padding: "5px 10px", borderRadius: 6, cursor: "pointer",
                    background: subCategory === sub.id ? "rgba(255,255,255,0.06)" : "transparent",
                    border: `1px solid ${subCategory === sub.id ? COLORS.border : "transparent"}`,
                    color: subCategory === sub.id ? COLORS.text : COLORS.textDim,
                    fontSize: 12, fontWeight: 500, fontFamily: "'DM Sans', sans-serif",
                  }}>{sub.label}</button>
                ))}
              </div>
            )}
          </div>

          {/* Vote Window */}
          <div style={sectionStyle}>
            <label style={labelStyle}>Vote Window *</label>
            <div style={{ display: "flex", gap: 8 }}>
              {[
                { id: "H6", label: "6H" },
                { id: "D1", label: "1D" },
                { id: "D3", label: "3D" },
              ].map((opt) => (
                <button
                  key={opt.id}
                  onClick={() => setVoteWindowType(opt.id)}
                  style={{
                    padding: "8px 14px", borderRadius: 8, cursor: "pointer",
                    border: `1px solid ${voteWindowType === opt.id ? COLORS.accent : COLORS.border}`,
                    background: voteWindowType === opt.id ? COLORS.accentGlow : "rgba(255,255,255,0.02)",
                    color: voteWindowType === opt.id ? COLORS.accentLight : COLORS.textDim,
                    fontSize: 13, fontWeight: 600, fontFamily: "'JetBrains Mono', monospace",
                  }}
                >
                  {opt.label}
                </button>
              ))}
            </div>
            <div style={hintStyle}>This maps to backend `voteWindowType` (H6 / D1 / D3).</div>
          </div>

          {/* Resolution */}
          <div style={sectionStyle}>
            <label style={labelStyle}>Resolution Method</label>
            <div style={{ display: "flex", gap: 8, marginBottom: 14 }}>
              {[
                { id: "auto", label: "Auto (Event/Date)", desc: "Resolves automatically based on verifiable source" },
                { id: "manual", label: "Manual (Your criteria)", desc: "You define custom resolution criteria" },
              ].map(r => (
                <button key={r.id} onClick={() => setResolutionType(r.id)} style={{
                  flex: 1, padding: 12, borderRadius: 10, cursor: "pointer",
                  background: resolutionType === r.id ? COLORS.accentGlow : "rgba(255,255,255,0.02)",
                  border: `1.5px solid ${resolutionType === r.id ? "rgba(124,58,237,0.3)" : COLORS.border}`,
                  textAlign: "left", transition: "all 0.15s",
                }}>
                  <div style={{ fontSize: 13, fontWeight: 600, color: resolutionType === r.id ? COLORS.accentLight : COLORS.text, marginBottom: 4 }}>{r.label}</div>
                  <div style={{ fontSize: 11, color: COLORS.textDim }}>{r.desc}</div>
                </button>
              ))}
            </div>
            {resolutionType === "auto" ? (
              <div>
                <input
                  value={resolutionSource} onChange={e => setResolutionSource(e.target.value)}
                  placeholder="e.g. CoinGecko price, Official election results..."
                  style={inputStyle}
                />
                <div style={hintStyle}>What data source will determine the outcome?</div>
              </div>
            ) : (
              <div>
                <textarea
                  value={resolutionCriteria} onChange={e => setResolutionCriteria(e.target.value)}
                  placeholder="Describe exactly how this question should be resolved..."
                  rows={3} style={{ ...inputStyle, resize: "vertical" }}
                />
                <div style={hintStyle}>Be as specific as possible for fair resolution</div>
              </div>
            )}
          </div>

          {/* Image Upload */}
          <div style={sectionStyle}>
            <label style={labelStyle}>Thumbnail Image <span style={{ color: COLORS.textDim, fontWeight: 400 }}>(optional)</span></label>
            <div style={{
              border: `2px dashed ${COLORS.border}`, borderRadius: 12,
              padding: imagePreview ? 0 : 30,
              textAlign: "center", cursor: "pointer", overflow: "hidden",
              transition: "border-color 0.15s",
            }}
            onClick={() => { const el = document.createElement("input"); el.type = "file"; el.accept = "image/*"; el.onchange = (e) => { const f = (e.target as HTMLInputElement).files?.[0]; if (f) setImagePreview(URL.createObjectURL(f)); }; el.click(); }}
            >
              {imagePreview ? (
                <div style={{ position: "relative" }}>
                  <img src={imagePreview} alt="Preview" style={{ width: "100%", maxHeight: 200, objectFit: "cover" }} />
                  <button onClick={e => { e.stopPropagation(); setImagePreview(null); }} style={{
                    position: "absolute", top: 8, right: 8, width: 28, height: 28, borderRadius: "50%",
                    background: "rgba(0,0,0,0.7)", border: "none", cursor: "pointer",
                    color: "white", display: "flex", alignItems: "center", justifyContent: "center",
                  }}>
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M18 6L6 18M6 6l12 12"/></svg>
                  </button>
                </div>
              ) : (
                <>
                  <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke={COLORS.textDim} strokeWidth="1.5"><rect x="3" y="3" width="18" height="18" rx="2"/><circle cx="8.5" cy="8.5" r="1.5"/><path d="M21 15l-5-5L5 21"/></svg>
                  <div style={{ fontSize: 13, color: COLORS.textDim, marginTop: 8 }}>Click to upload image</div>
                  <div style={{ fontSize: 11, color: COLORS.textDim }}>PNG, JPG up to 2MB</div>
                </>
              )}
            </div>
          </div>

          {/* Tags */}
          <div style={sectionStyle}>
            <label style={labelStyle}>Tags <span style={{ color: COLORS.textDim, fontWeight: 400 }}>(up to 5)</span></label>
            <div style={{ display: "flex", flexWrap: "wrap", gap: 6, marginBottom: tags.length > 0 ? 10 : 0 }}>
              {tags.map((t, i) => (
                <span key={i} style={{
                  display: "flex", alignItems: "center", gap: 4,
                  padding: "4px 10px", borderRadius: 6,
                  background: COLORS.accentGlow, color: COLORS.accentLight,
                  fontSize: 12, fontWeight: 500,
                }}>
                  #{t}
                  <button onClick={() => removeTag(i)} style={{ background: "none", border: "none", cursor: "pointer", color: COLORS.accentLight, padding: 0, display: "flex" }}>
                    <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M18 6L6 18M6 6l12 12"/></svg>
                  </button>
                </span>
              ))}
            </div>
            <div style={{ display: "flex", gap: 8 }}>
              <input
                value={tagInput} onChange={e => setTagInput(e.target.value)}
                onKeyDown={e => { if (e.key === "Enter") { e.preventDefault(); addTag(); } }}
                placeholder="Add tag..."
                style={{ ...inputStyle, flex: 1 }}
              />
              <button onClick={addTag} disabled={!tagInput.trim() || tags.length >= 5} style={{
                padding: "0 16px", borderRadius: 10,
                background: tagInput.trim() && tags.length < 5 ? COLORS.accentGlow : "rgba(255,255,255,0.03)",
                border: "none", color: tagInput.trim() && tags.length < 5 ? COLORS.accentLight : COLORS.textDim,
                cursor: tagInput.trim() && tags.length < 5 ? "pointer" : "not-allowed",
                fontSize: 13, fontWeight: 600,
              }}>Add</button>
            </div>
          </div>

          {false && (
            <div style={{
              ...sectionStyle,
              background: boost ? "rgba(124,58,237,0.06)" : COLORS.surface,
              border: `1px solid ${boost ? "rgba(124,58,237,0.3)" : COLORS.border}`,
            }}>
              <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                <div>
                  <div style={{ fontSize: 14, fontWeight: 600, color: COLORS.text, marginBottom: 4 }}>
                    ✨ Premium Boost
                  </div>
                  <div style={{ fontSize: 12, color: COLORS.textDim }}>
                    Feature your question — higher visibility, priority in feed, badge
                  </div>
                </div>
                <button onClick={() => setBoost(!boost)} style={{
                  width: 48, height: 26, borderRadius: 13, cursor: "pointer",
                  background: boost ? COLORS.accent : "rgba(255,255,255,0.1)",
                  border: "none", padding: 2, transition: "background 0.2s",
                  position: "relative",
                }}>
                  <div style={{
                    width: 22, height: 22, borderRadius: "50%", background: "white",
                    transition: "transform 0.2s",
                    transform: boost ? "translateX(22px)" : "translateX(0)",
                  }} />
                </button>
              </div>
              {boost && (
                <div style={{
                  marginTop: 12, padding: "8px 12px", borderRadius: 8,
                  background: "rgba(124,58,237,0.08)",
                  fontSize: 12, color: COLORS.accentLight, fontWeight: 500,
                }}>
                  💰 5 credits will be charged for premium boost
                </div>
              )}
            </div>
          )}

          {/* Fee Split */}
          <div style={sectionStyle}>
            <label style={labelStyle}>Fee Distribution</label>
            <div style={{ fontSize: 12, color: COLORS.textDim, marginBottom: 12 }}>
              Platform fee is fixed at {PLATFORM_FEE_PERCENT}%. You set how the remaining {DISTRIBUTABLE_FEE_PERCENT}% is split.
            </div>
            <div style={{ display: "flex", gap: 8, marginBottom: 12 }}>
              <div style={{
                flex: 1, borderRadius: 8, border: `1px solid ${COLORS.border}`, padding: "10px 12px",
                background: "rgba(255,255,255,0.02)",
              }}>
                <div style={{ fontSize: 11, color: COLORS.textDim, marginBottom: 4 }}>Platform</div>
                <div style={{ fontSize: 16, fontWeight: 700, color: COLORS.text }}>20%</div>
              </div>
              <div style={{
                flex: 1, borderRadius: 8, border: `1px solid ${COLORS.border}`, padding: "10px 12px",
                background: COLORS.accentGlow,
              }}>
                <div style={{ fontSize: 11, color: COLORS.textDim, marginBottom: 4 }}>Creator</div>
                <div style={{ fontSize: 16, fontWeight: 700, color: COLORS.accentLight }}>{creatorFeePercent}%</div>
              </div>
              <div style={{
                flex: 1, borderRadius: 8, border: `1px solid ${COLORS.border}`, padding: "10px 12px",
                background: "rgba(34,197,94,0.08)",
              }}>
                <div style={{ fontSize: 11, color: COLORS.textDim, marginBottom: 4 }}>Voters</div>
                <div style={{ fontSize: 16, fontWeight: 700, color: COLORS.green }}>{voterFeePercent}%</div>
              </div>
            </div>
            <div style={{ display: "flex", gap: 6, flexWrap: "wrap" }}>
              {[0, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100].map((v) => (
                <button
                  key={v}
                  onClick={() => setCreatorSplit(v)}
                  style={{
                    padding: "5px 10px", borderRadius: 6, cursor: "pointer",
                    border: `1px solid ${creatorSplit === v ? COLORS.accent : COLORS.border}`,
                    background: creatorSplit === v ? COLORS.accentGlow : "transparent",
                    color: creatorSplit === v ? COLORS.accentLight : COLORS.textDim,
                    fontSize: 12, fontWeight: 600, fontFamily: "'JetBrains Mono', monospace",
                  }}
                >
                  {v}%
                </button>
              ))}
            </div>
            <div style={hintStyle}>`creatorSplitInPool`: {creatorSplit}% of distributable fee ({DISTRIBUTABLE_FEE_PERCENT}%).</div>
          </div>

          {/* Submit */}
          <div style={{ display: "flex", gap: 12, marginTop: 8 }}>
            <button onClick={() => { if (isValid) setStep(2); }} disabled={!isValid} style={{
              flex: 1, padding: "14px 0", borderRadius: 10,
              background: isValid ? `linear-gradient(135deg, ${COLORS.accent}, #5B21B6)` : COLORS.textDim,
              border: "none", color: "white",
              cursor: isValid ? "pointer" : "not-allowed",
              fontSize: 15, fontWeight: 700, fontFamily: "'DM Sans', sans-serif",
              boxShadow: isValid ? "0 4px 16px rgba(124,58,237,0.3)" : "none",
            }}>Preview & Submit</button>
          </div>
          {!isValid && title.length > 0 && (
            <div style={{ fontSize: 12, color: COLORS.red, marginTop: 8, textAlign: "center" }}>
              {title.length < 10 ? "Question title must be at least 10 characters" : !category ? "Please select a category" : "Please fill resolution information"}
            </div>
          )}
        </div>
      )}
    </div>
  );
}

export default QuestionSubmitView;

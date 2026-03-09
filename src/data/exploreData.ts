const EXPLORE_CATEGORIES = [
  { id: "live", label: "LIVE", subs: [] },
  { id: "trending", label: "Trending", subs: [] },
  { id: "politics", label: "Politics", subs: [
    { id: "us-politics", label: "US Politics" },
    { id: "kr-politics", label: "Korea" },
    { id: "eu-politics", label: "Europe" },
    { id: "global-politics", label: "Global" },
  ]},
  { id: "crypto", label: "Crypto", subs: [
    { id: "bitcoin", label: "Bitcoin" },
    { id: "ethereum", label: "Ethereum" },
    { id: "altcoins", label: "Altcoins" },
    { id: "defi", label: "DeFi" },
    { id: "regulation", label: "Regulation" },
  ]},
  { id: "sports", label: "Sports", subs: [
    { id: "football", label: "Football", subs2: ["EPL", "La Liga", "Serie A", "Bundesliga", "K-League"] },
    { id: "basketball", label: "Basketball", subs2: ["NBA", "KBL", "EuroLeague"] },
    { id: "baseball", label: "Baseball", subs2: ["MLB", "KBO", "NPB"] },
    { id: "mma", label: "MMA / Boxing" },
    { id: "esports", label: "Esports" },
    { id: "f1", label: "F1" },
  ]},
  { id: "tech", label: "Tech & AI", subs: [
    { id: "ai-models", label: "AI Models" },
    { id: "big-tech", label: "Big Tech" },
    { id: "startups", label: "Startups" },
    { id: "hardware", label: "Hardware" },
  ]},
  { id: "economy", label: "Economy", subs: [
    { id: "fed", label: "Fed / Rates" },
    { id: "markets", label: "Stock Market" },
    { id: "inflation", label: "Inflation" },
    { id: "housing", label: "Housing" },
  ]},
  { id: "culture", label: "Culture", subs: [
    { id: "entertainment", label: "Entertainment" },
    { id: "music", label: "Music" },
    { id: "social-media", label: "Social Media" },
  ]},
  { id: "expired", label: "Expired", subs: [] },
];

export { EXPLORE_CATEGORIES };

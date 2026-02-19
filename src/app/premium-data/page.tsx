'use client';

import React, { useState } from 'react';
import { Download, Filter, Eye, Database, CheckCircle, XCircle } from 'lucide-react';
import { API_BASE_URL } from '@/lib/api';

interface PremiumDataRequest {
  questionId: number;
  countryCode?: string;
  jobCategory?: string;
  ageGroup?: number;
  minTier?: string;
  minLatencyMs?: number;
}

interface PremiumDataPoint {
  voteId: number;
  questionTitle: string;
  choice: string;
  latencyMs: number;
  countryCode: string;
  jobCategory?: string;
  ageGroup?: number;
  tier: string;
  tierWeight: number;
  timestamp: string;
}

interface PremiumDataResponse {
  questionId: number;
  questionTitle: string;
  filters: PremiumDataRequest;
  totalCount: number;
  yesCount: number;
  noCount: number;
  yesPercentage: number;
  noPercentage: number;
  data: PremiumDataPoint[];
}

export default function PremiumDataExport() {
  const [questionId] = useState(1); // Fixed to question 1 for now
  const [filters, setFilters] = useState<PremiumDataRequest>({
    questionId: 1,
    minLatencyMs: 5000 // Default: 5 seconds or more (high quality)
  });

  const [previewData, setPreviewData] = useState<PremiumDataResponse | null>(null);
  const [loading, setLoading] = useState(false);

  const handlePreview = async () => {
    setLoading(true);
    try {
      const response = await fetch(`${API_BASE_URL}/premium-data/preview`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(filters)
      });
      const data = await response.json();
      setPreviewData(data);
    } catch (error) {
      if (process.env.NODE_ENV === 'development') {
        console.error('Failed to preview:', error);
      }
      alert('Preview failed.');
    } finally {
      setLoading(false);
    }
  };

  const handleExport = async (format: 'json' | 'csv') => {
    setLoading(true);
    try {
      const response = await fetch(`${API_BASE_URL}/premium-data/export`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(filters)
      });
      const data: PremiumDataResponse = await response.json();

      if (format === 'json') {
        downloadJSON(data);
      } else {
        downloadCSV(data);
      }
    } catch (error) {
      if (process.env.NODE_ENV === 'development') {
        console.error('Failed to export:', error);
      }
      alert('Download failed.');
    } finally {
      setLoading(false);
    }
  };

  const downloadJSON = (data: PremiumDataResponse) => {
    const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `premium-data-${questionId}-${Date.now()}.json`;
    a.click();
  };

  const downloadCSV = (data: PremiumDataResponse) => {
    const headers = ['voteId', 'choice', 'latencyMs', 'countryCode', 'jobCategory', 'ageGroup', 'tier', 'tierWeight', 'timestamp'];
    const rows = data.data.map(d => [
      d.voteId,
      d.choice,
      d.latencyMs,
      d.countryCode,
      d.jobCategory || '',
      d.ageGroup || '',
      d.tier,
      d.tierWeight,
      d.timestamp
    ]);

    const csv = [
      headers.join(','),
      ...rows.map(row => row.join(','))
    ].join('\n');

    const blob = new Blob([csv], { type: 'text/csv' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `premium-data-${questionId}-${Date.now()}.csv`;
    a.click();
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-50 to-purple-50 py-8 px-4">
      <div className="max-w-6xl mx-auto">
        
        {/* Header */}
        <div className="mb-8">
          <div className="flex items-center gap-3 mb-2">
            <Database className="h-8 w-8 text-purple-600" />
            <h1 className="text-4xl font-black text-slate-800">Premium Data Export</h1>
          </div>
          <p className="text-slate-600 text-lg">Filter and download high-quality prediction data</p>
        </div>

        {/* Filter Section */}
        <div className="bg-white rounded-xl shadow-lg p-6 mb-6">
          <div className="flex items-center gap-2 mb-4">
            <Filter className="h-6 w-6 text-purple-600" />
            <h2 className="text-2xl font-bold">Filter Settings</h2>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            {/* Country */}
            <div>
              <label className="block text-sm font-semibold text-slate-700 mb-2">Country</label>
              <select
                value={filters.countryCode || ''}
                onChange={(e) => setFilters({ ...filters, countryCode: e.target.value || undefined })}
                className="w-full p-3 border border-slate-300 rounded-lg focus:ring-2 focus:ring-purple-500"
              >
                <option value="">All</option>
                <option value="KR">Korea</option>
                <option value="US">USA</option>
                <option value="JP">Japan</option>
                <option value="SG">Singapore</option>
                <option value="VN">Vietnam</option>
              </select>
            </div>

            {/* Job */}
            <div>
              <label className="block text-sm font-semibold text-slate-700 mb-2">Job</label>
              <select
                value={filters.jobCategory || ''}
                onChange={(e) => setFilters({ ...filters, jobCategory: e.target.value || undefined })}
                className="w-full p-3 border border-slate-300 rounded-lg focus:ring-2 focus:ring-purple-500"
              >
                <option value="">All</option>
                <option value="IT">IT</option>
                <option value="Finance">Finance</option>
                <option value="Medical">Medical</option>
                <option value="Student">Student</option>
                <option value="Service">Service</option>
              </select>
            </div>

            {/* Min Response Time (Quality Filter) */}
            <div>
              <label className="block text-sm font-semibold text-slate-700 mb-2">Min Response Time (Anti-Abuse)</label>
              <select
                value={filters.minLatencyMs || ''}
                onChange={(e) => setFilters({ ...filters, minLatencyMs: e.target.value ? Number(e.target.value) : undefined })}
                className="w-full p-3 border border-slate-300 rounded-lg focus:ring-2 focus:ring-purple-500"
              >
                <option value="">All</option>
                <option value="2000">2 seconds or more (Normal)</option>
                <option value="5000">5 seconds or more (High Quality)</option>
                <option value="10000">10 seconds or more (Best Quality)</option>
              </select>
            </div>
          </div>

          <div className="mt-4 flex gap-3">
            <button
              onClick={handlePreview}
              disabled={loading}
              className="flex items-center gap-2 px-6 py-3 bg-purple-600 text-white rounded-lg hover:bg-purple-700 transition font-semibold disabled:opacity-50"
            >
              <Eye size={20} />
              Preview
            </button>
            <button
              onClick={() => handleExport('json')}
              disabled={loading}
              className="flex items-center gap-2 px-6 py-3 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition font-semibold disabled:opacity-50"
            >
              <Download size={20} />
              Download JSON
            </button>
            <button
              onClick={() => handleExport('csv')}
              disabled={loading}
              className="flex items-center gap-2 px-6 py-3 bg-green-600 text-white rounded-lg hover:bg-green-700 transition font-semibold disabled:opacity-50"
            >
              <Download size={20} />
              Download CSV
            </button>
          </div>
        </div>

        {/* Preview Results */}
        {previewData && (
          <div className="bg-white rounded-xl shadow-lg p-6">
            <h2 className="text-2xl font-bold mb-4">Data Preview</h2>

            {/* Statistics */}
            <div className="grid grid-cols-4 gap-4 mb-6">
              <div className="p-4 bg-purple-50 rounded-lg">
                <p className="text-sm text-slate-600">Total Data</p>
                <p className="text-2xl font-bold text-purple-600">{previewData.totalCount.toLocaleString()}</p>
              </div>
              <div className="p-4 bg-green-50 rounded-lg">
                <p className="text-sm text-slate-600">YES</p>
                <p className="text-2xl font-bold text-green-600">{previewData.yesPercentage.toFixed(1)}%</p>
              </div>
              <div className="p-4 bg-red-50 rounded-lg">
                <p className="text-sm text-slate-600">NO</p>
                <p className="text-2xl font-bold text-red-600">{previewData.noPercentage.toFixed(1)}%</p>
              </div>
              <div className="p-4 bg-blue-50 rounded-lg">
                <p className="text-sm text-slate-600">Quality</p>
                <p className="text-2xl font-bold text-blue-600">
                  {previewData.filters.minLatencyMs ? `${previewData.filters.minLatencyMs / 1000}s+` : 'All'}
                </p>
              </div>
            </div>

            {/* Data Table */}
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead className="bg-slate-100">
                  <tr>
                    <th className="p-3 text-left font-semibold">Choice</th>
                    <th className="p-3 text-left font-semibold">Response Time</th>
                    <th className="p-3 text-left font-semibold">Country</th>
                    <th className="p-3 text-left font-semibold">Job</th>
                    <th className="p-3 text-left font-semibold">Age</th>
                    <th className="p-3 text-left font-semibold">Tier</th>
                  </tr>
                </thead>
                <tbody>
                  {previewData.data.slice(0, 10).map((item, idx) => (
                    <tr key={idx} className="border-b hover:bg-slate-50">
                      <td className="p-3">
                        <span className={`px-2 py-1 rounded text-xs font-bold ${item.choice === 'YES' ? 'bg-green-100 text-green-700' : 'bg-red-100 text-red-700'}`}>
                          {item.choice}
                        </span>
                      </td>
                      <td className="p-3 text-slate-600">{(item.latencyMs / 1000).toFixed(1)}s</td>
                      <td className="p-3 text-slate-600">{item.countryCode}</td>
                      <td className="p-3 text-slate-600">{item.jobCategory || '-'}</td>
                      <td className="p-3 text-slate-600">{item.ageGroup ? `${item.ageGroup}` : '-'}</td>
                      <td className="p-3">
                        <span className={`px-2 py-1 rounded text-xs font-bold ${
                          item.tier === 'PLATINUM' ? 'bg-purple-100 text-purple-700' :
                          item.tier === 'GOLD' ? 'bg-yellow-100 text-yellow-700' :
                          item.tier === 'SILVER' ? 'bg-slate-200 text-slate-700' :
                          'bg-orange-100 text-orange-700'
                        }`}>
                          {item.tier}
                        </span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            <p className="text-sm text-slate-500 mt-4">
              * Preview shows up to 10 items. Click download button to get full data.
            </p>
          </div>
        )}
      </div>
    </div>
  );
}

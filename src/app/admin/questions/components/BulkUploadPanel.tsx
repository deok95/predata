'use client';

import { useState } from 'react';
import type { BulkQuestionItem, BulkUploadResult } from '@/types/admin';
import { API_BASE_URL, authFetch } from '@/lib/api';

export default function BulkUploadPanel() {
  const [jsonInput, setJsonInput] = useState('');
  const [parsedQuestions, setParsedQuestions] = useState<BulkQuestionItem[]>([]);
  const [uploadResult, setUploadResult] = useState<BulkUploadResult | null>(null);
  const [isUploading, setIsUploading] = useState(false);

  const handleParse = () => {
    try {
      const parsed = JSON.parse(jsonInput);
      const questions = Array.isArray(parsed) ? parsed : [parsed];
      setParsedQuestions(questions);
      setUploadResult(null);
    } catch (e) {
      alert('Invalid JSON format. Please check your input.');
    }
  };

  const handleUpload = async () => {
    if (parsedQuestions.length === 0) {
      alert('Please parse questions first');
      return;
    }

    setIsUploading(true);
    try {
      const response = await authFetch(`${API_BASE_URL}/admin/questions/bulk`, {
        method: 'POST',
        body: JSON.stringify(parsedQuestions),
      });

      const data = await response.json();
      if (!response.ok || data?.success === false) {
        throw new Error(data?.message || `HTTP ${response.status}`);
      }
      setUploadResult(data.data || data || null);
    } catch (error) {
      alert('Upload failed: ' + (error instanceof Error ? error.message : 'Unknown error'));
    } finally {
      setIsUploading(false);
    }
  };

  return (
    <div className="bulk-upload-panel bg-white dark:bg-slate-800 rounded-lg p-6 shadow-lg">
      <h3 className="text-xl font-bold mb-4">Bulk Upload Questions</h3>

      {/* JSON Input */}
      <div className="mb-4">
        <label className="block text-sm font-medium mb-2">JSON Input</label>
        <textarea
          value={jsonInput}
          onChange={(e) => setJsonInput(e.target.value)}
          placeholder='[{"title":"Will BTC reach $100k?","category":"ECONOMY","bettingDuration":"24h","seedUsdc":1000}]'
          rows={10}
          className="w-full font-mono text-sm p-3 border rounded-lg dark:bg-slate-700 dark:border-slate-600"
        />
      </div>

      {/* Parse Button */}
      <div className="flex gap-2 mb-4">
        <button
          onClick={handleParse}
          className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition"
        >
          Parse JSON
        </button>
      </div>

      {/* Preview Table */}
      {parsedQuestions.length > 0 && (
        <div className="mb-4">
          <h4 className="font-semibold mb-2">Preview ({parsedQuestions.length} questions)</h4>
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead className="bg-slate-100 dark:bg-slate-700">
                <tr>
                  <th className="px-3 py-2 text-left">Title</th>
                  <th className="px-3 py-2 text-left">Category</th>
                  <th className="px-3 py-2 text-left">Duration</th>
                  <th className="px-3 py-2 text-left">Seed</th>
                </tr>
              </thead>
              <tbody>
                {parsedQuestions.map((q, i) => (
                  <tr key={i} className="border-b dark:border-slate-700">
                    <td className="px-3 py-2">{q.title.substring(0, 50)}...</td>
                    <td className="px-3 py-2">{q.category || 'SPORTS'}</td>
                    <td className="px-3 py-2">{q.bettingDuration || '24h'}</td>
                    <td className="px-3 py-2">${q.seedUsdc || 1000}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {/* Upload Button */}
          <button
            onClick={handleUpload}
            disabled={isUploading}
            className="mt-4 px-6 py-3 bg-green-600 text-white rounded-lg hover:bg-green-700 transition disabled:opacity-50"
          >
            {isUploading ? 'Uploading...' : `Create ${parsedQuestions.length} Question${parsedQuestions.length > 1 ? 's' : ''}`}
          </button>
        </div>
      )}

      {/* Upload Result */}
      {uploadResult && (
        <div className="mt-4 p-4 bg-slate-100 dark:bg-slate-700 rounded-lg">
          <h4 className="font-semibold mb-2">Upload Result</h4>
          <p className="text-green-600 font-medium">
            ✓ Created: {uploadResult.created} / Failed: {uploadResult.failed}
          </p>
          {uploadResult.results && uploadResult.results.filter(r => !r.success).length > 0 && (
            <div className="mt-2">
              <p className="text-red-600 font-medium">Failed questions:</p>
              <ul className="text-sm mt-1">
                {uploadResult.results.filter(r => !r.success).map((r, i) => (
                  <li key={i} className="text-red-500">
                    {r.title}: {r.error}
                  </li>
                ))}
              </ul>
            </div>
          )}
        </div>
      )}
    </div>
  );
}

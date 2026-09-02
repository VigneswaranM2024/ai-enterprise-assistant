import React, { useState } from 'react';
import { utilityService } from '../services/utilityService';
import { Terminal, Mail, Database, Code, Copy, Check, RefreshCw } from 'lucide-react';

export const AiUtilitiesPage: React.FC = () => {
  const [activeTab, setActiveTab] = useState<'email' | 'sql' | 'code'>('email');
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState('');
  const [copied, setCopied] = useState(false);

  // Email state
  const [emailContext, setEmailContext] = useState('');
  const [emailTone, setEmailTone] = useState('Professional');
  const [emailAudience, setEmailAudience] = useState('Colleagues');

  // SQL state
  const [sqlPrompt, setSqlPrompt] = useState('');
  const [sqlSchema, setSqlSchema] = useState('');
  const [sqlDialect, setSqlDialect] = useState('PostgreSQL');

  // Code state
  const [codePrompt, setCodePrompt] = useState('');
  const [codeLanguage, setCodeLanguage] = useState('TypeScript');

  const handleCopy = () => {
    navigator.clipboard.writeText(result);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const handleEmailSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!emailContext) return;
    setLoading(true);
    try {
      const res = await utilityService.generateEmail({
        context: emailContext,
        tone: emailTone,
        targetAudience: emailAudience
      });
      setResult(res.result);
    } catch (error) {
      console.error(error);
      setResult('Failed to generate email.');
    } finally {
      setLoading(false);
    }
  };

  const handleSqlSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!sqlPrompt) return;
    setLoading(true);
    try {
      const res = await utilityService.generateSql({
        prompt: sqlPrompt,
        schemaContext: sqlSchema,
        dialect: sqlDialect
      });
      setResult(res.result);
    } catch (error) {
      console.error(error);
      setResult('Failed to generate SQL.');
    } finally {
      setLoading(false);
    }
  };

  const handleCodeSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!codePrompt) return;
    setLoading(true);
    try {
      const res = await utilityService.generateCode({
        prompt: codePrompt,
        language: codeLanguage
      });
      setResult(res.result);
    } catch (error) {
      console.error(error);
      setResult('Failed to generate code.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="space-y-6 h-full flex flex-col">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-white flex items-center space-x-2">
            <Terminal className="w-6 h-6 text-emerald-400" />
            <span>AI Utilities</span>
          </h1>
          <p className="text-xs text-slate-400">
            Purpose-built generative AI tools for enterprise productivity.
          </p>
        </div>
      </div>

      {/* Tabs */}
      <div className="flex space-x-2 border-b border-slate-800 pb-2">
        <button
          onClick={() => { setActiveTab('email'); setResult(''); }}
          className={`px-4 py-2 text-xs font-semibold rounded-lg transition flex items-center ${activeTab === 'email' ? 'bg-emerald-600/20 text-emerald-400' : 'text-slate-400 hover:text-slate-200 hover:bg-slate-900'}`}
        >
          <Mail className="w-4 h-4 mr-2" /> Email Generator
        </button>
        <button
          onClick={() => { setActiveTab('sql'); setResult(''); }}
          className={`px-4 py-2 text-xs font-semibold rounded-lg transition flex items-center ${activeTab === 'sql' ? 'bg-emerald-600/20 text-emerald-400' : 'text-slate-400 hover:text-slate-200 hover:bg-slate-900'}`}
        >
          <Database className="w-4 h-4 mr-2" /> SQL Query Builder
        </button>
        <button
          onClick={() => { setActiveTab('code'); setResult(''); }}
          className={`px-4 py-2 text-xs font-semibold rounded-lg transition flex items-center ${activeTab === 'code' ? 'bg-emerald-600/20 text-emerald-400' : 'text-slate-400 hover:text-slate-200 hover:bg-slate-900'}`}
        >
          <Code className="w-4 h-4 mr-2" /> Code Generator
        </button>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 flex-1 min-h-0">
        {/* Left Column: Input Form */}
        <div className="glass-panel rounded-xl border-slate-800 p-6 flex flex-col overflow-y-auto">
          {activeTab === 'email' && (
            <form onSubmit={handleEmailSubmit} className="space-y-4">
              <h2 className="text-sm font-bold text-white mb-4">Email Configuration</h2>
              <div>
                <label className="block text-xs font-semibold text-slate-300 mb-1">Context / Goal</label>
                <textarea
                  required
                  value={emailContext}
                  onChange={(e) => setEmailContext(e.target.value)}
                  placeholder="e.g., Explain the delay in Q3 deliverables due to API issues..."
                  className="w-full bg-slate-900 border border-slate-800 rounded-lg px-3 py-2 text-xs text-white h-32 focus:outline-none focus:border-emerald-500"
                />
              </div>
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-semibold text-slate-300 mb-1">Tone</label>
                  <select value={emailTone} onChange={e => setEmailTone(e.target.value)} className="w-full bg-slate-900 border border-slate-800 rounded-lg px-3 py-2 text-xs text-white">
                    <option>Professional</option>
                    <option>Friendly</option>
                    <option>Urgent</option>
                    <option>Apologetic</option>
                    <option>Persuasive</option>
                  </select>
                </div>
                <div>
                  <label className="block text-xs font-semibold text-slate-300 mb-1">Audience</label>
                  <select value={emailAudience} onChange={e => setEmailAudience(e.target.value)} className="w-full bg-slate-900 border border-slate-800 rounded-lg px-3 py-2 text-xs text-white">
                    <option>Colleagues</option>
                    <option>Management / Execs</option>
                    <option>Clients</option>
                    <option>Vendors</option>
                  </select>
                </div>
              </div>
              <button type="submit" disabled={loading || !emailContext} className="w-full py-2 px-4 bg-emerald-600 hover:bg-emerald-500 text-white font-semibold text-xs rounded-lg transition disabled:opacity-50 flex items-center justify-center">
                {loading ? <RefreshCw className="w-4 h-4 animate-spin mr-2" /> : <Mail className="w-4 h-4 mr-2" />}
                {loading ? 'Drafting...' : 'Generate Email'}
              </button>
            </form>
          )}

          {activeTab === 'sql' && (
            <form onSubmit={handleSqlSubmit} className="space-y-4">
              <h2 className="text-sm font-bold text-white mb-4">SQL Configuration</h2>
              <div>
                <label className="block text-xs font-semibold text-slate-300 mb-1">What do you want to query?</label>
                <textarea
                  required
                  value={sqlPrompt}
                  onChange={(e) => setSqlPrompt(e.target.value)}
                  placeholder="e.g., Get the total revenue per department for the last quarter..."
                  className="w-full bg-slate-900 border border-slate-800 rounded-lg px-3 py-2 text-xs text-white h-24 focus:outline-none focus:border-emerald-500"
                />
              </div>
              <div>
                <label className="block text-xs font-semibold text-slate-300 mb-1">Schema Context (Optional)</label>
                <textarea
                  value={sqlSchema}
                  onChange={(e) => setSqlSchema(e.target.value)}
                  placeholder="e.g., Table 'sales' has columns 'id, amount, dept_id, date'..."
                  className="w-full bg-slate-900 border border-slate-800 rounded-lg px-3 py-2 text-xs text-white h-20 focus:outline-none focus:border-emerald-500 font-mono text-[10px]"
                />
              </div>
              <div>
                <label className="block text-xs font-semibold text-slate-300 mb-1">SQL Dialect</label>
                <select value={sqlDialect} onChange={e => setSqlDialect(e.target.value)} className="w-full bg-slate-900 border border-slate-800 rounded-lg px-3 py-2 text-xs text-white">
                  <option>PostgreSQL</option>
                  <option>MySQL</option>
                  <option>SQL Server</option>
                  <option>Oracle</option>
                </select>
              </div>
              <button type="submit" disabled={loading || !sqlPrompt} className="w-full py-2 px-4 bg-emerald-600 hover:bg-emerald-500 text-white font-semibold text-xs rounded-lg transition disabled:opacity-50 flex items-center justify-center">
                {loading ? <RefreshCw className="w-4 h-4 animate-spin mr-2" /> : <Database className="w-4 h-4 mr-2" />}
                {loading ? 'Building Query...' : 'Generate SQL'}
              </button>
            </form>
          )}

          {activeTab === 'code' && (
            <form onSubmit={handleCodeSubmit} className="space-y-4">
              <h2 className="text-sm font-bold text-white mb-4">Code Generator Configuration</h2>
              <div>
                <label className="block text-xs font-semibold text-slate-300 mb-1">What should the code do?</label>
                <textarea
                  required
                  value={codePrompt}
                  onChange={(e) => setCodePrompt(e.target.value)}
                  placeholder="e.g., Write a function to recursively flatten an array of nested arrays..."
                  className="w-full bg-slate-900 border border-slate-800 rounded-lg px-3 py-2 text-xs text-white h-32 focus:outline-none focus:border-emerald-500"
                />
              </div>
              <div>
                <label className="block text-xs font-semibold text-slate-300 mb-1">Language / Framework</label>
                <input
                  type="text"
                  value={codeLanguage}
                  onChange={(e) => setCodeLanguage(e.target.value)}
                  placeholder="e.g., TypeScript, Python, React, Java"
                  className="w-full bg-slate-900 border border-slate-800 rounded-lg px-3 py-2 text-xs text-white focus:outline-none focus:border-emerald-500"
                />
              </div>
              <button type="submit" disabled={loading || !codePrompt} className="w-full py-2 px-4 bg-emerald-600 hover:bg-emerald-500 text-white font-semibold text-xs rounded-lg transition disabled:opacity-50 flex items-center justify-center">
                {loading ? <RefreshCw className="w-4 h-4 animate-spin mr-2" /> : <Code className="w-4 h-4 mr-2" />}
                {loading ? 'Generating...' : 'Generate Code'}
              </button>
            </form>
          )}
        </div>

        {/* Right Column: Output */}
        <div className="glass-panel rounded-xl border-slate-800 flex flex-col overflow-hidden relative">
          <div className="flex justify-between items-center p-3 border-b border-slate-800 bg-slate-900/80">
            <h2 className="text-xs font-bold text-slate-300">Generated Output</h2>
            {result && (
              <button onClick={handleCopy} className="text-slate-400 hover:text-white transition flex items-center space-x-1 bg-slate-800 px-2 py-1 rounded text-[10px]">
                {copied ? <Check className="w-3 h-3 text-emerald-400" /> : <Copy className="w-3 h-3" />}
                <span>{copied ? 'Copied' : 'Copy'}</span>
              </button>
            )}
          </div>
          <div className="flex-1 overflow-y-auto p-4 bg-slate-950">
            {loading ? (
              <div className="h-full flex flex-col items-center justify-center text-slate-500 space-y-4">
                <RefreshCw className="w-8 h-8 animate-spin text-emerald-400" />
                <p className="text-xs">AI is working on your request...</p>
              </div>
            ) : result ? (
              <pre className="text-xs text-slate-300 font-mono whitespace-pre-wrap leading-relaxed">
                {result}
              </pre>
            ) : (
              <div className="h-full flex flex-col items-center justify-center text-slate-500">
                <p className="text-xs">Output will appear here.</p>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};

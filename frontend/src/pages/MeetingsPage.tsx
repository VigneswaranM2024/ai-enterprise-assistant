import React, { useEffect, useState } from 'react';
import { meetingService } from '../services/meetingService';
import { MeetingResponse } from '../types/meeting.types';
import { useAuth } from '../hooks/useAuth';
import { Users, UploadCloud, X, Trash2, Calendar, FileText, CheckCircle, Clock, AlertTriangle } from 'lucide-react';

export const MeetingsPage: React.FC = () => {
  const { isAdmin } = useAuth();
  const [meetings, setMeetings] = useState<MeetingResponse[]>([]);
  const [loading, setLoading] = useState(true);

  // Upload Modal State
  const [isUploadOpen, setIsUploadOpen] = useState(false);
  const [uploadFile, setUploadFile] = useState<File | null>(null);
  const [uploadTitle, setUploadTitle] = useState('');
  const [uploadDate, setUploadDate] = useState(new Date().toISOString().split('T')[0]);
  const [uploading, setUploading] = useState(false);

  // Detail Modal State
  const [selectedMeeting, setSelectedMeeting] = useState<MeetingResponse | null>(null);

  const fetchMeetings = async () => {
    setLoading(true);
    try {
      const data = await meetingService.getMeetings();
      setMeetings(data.content);
    } catch (error) {
      console.error('Failed to fetch meetings:', error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchMeetings();
  }, []);

  const handleUploadSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!uploadFile || !uploadTitle || !uploadDate) return;

    setUploading(true);
    try {
      const formData = new FormData();
      formData.append('file', uploadFile);
      formData.append('title', uploadTitle);
      formData.append('meetingDate', uploadDate);

      await meetingService.uploadTranscript(formData);
      setIsUploadOpen(false);
      setUploadFile(null);
      setUploadTitle('');
      fetchMeetings();
    } catch (error: any) {
      console.error('Transcript upload failed:', error);
      alert(error.response?.data?.message || 'Failed to upload transcript. See console for details.');
    } finally {
      setUploading(false);
    }
  };

  const handleDelete = async (e: React.MouseEvent, id: string) => {
    e.stopPropagation();
    if (window.confirm('Are you sure you want to delete this meeting?')) {
      try {
        await meetingService.deleteMeeting(id);
        if (selectedMeeting?.id === id) {
          setSelectedMeeting(null);
        }
        fetchMeetings();
      } catch (error) {
        console.error('Delete failed:', error);
      }
    }
  };

  return (
    <div className="space-y-6 h-full flex flex-col">
      {/* Header Banner */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-white flex items-center space-x-2">
            <Users className="w-6 h-6 text-emerald-400" />
            <span>Meeting Intelligence</span>
          </h1>
          <p className="text-xs text-slate-400">
            Upload transcripts to extract AI-generated summaries, decisions, action items, and risks.
          </p>
        </div>

        <button
          onClick={() => setIsUploadOpen(true)}
          className="px-4 py-2 bg-emerald-600 hover:bg-emerald-500 text-white font-semibold text-xs rounded-lg transition inline-flex items-center space-x-2 shadow-lg shadow-emerald-900/20"
        >
          <UploadCloud className="w-4 h-4" />
          <span>Upload Transcript</span>
        </button>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 flex-1 min-h-0">
        {/* Left Column: Meeting List */}
        <div className="lg:col-span-1 glass-panel rounded-xl border-slate-800 flex flex-col overflow-hidden">
          <div className="p-4 border-b border-slate-800 bg-slate-900/50">
            <h2 className="text-sm font-bold text-white flex items-center space-x-2">
              <Calendar className="w-4 h-4 text-emerald-400" />
              <span>Recent Meetings</span>
            </h2>
          </div>
          <div className="overflow-y-auto flex-1 p-2 space-y-2">
            {loading ? (
              <div className="text-center p-8 text-slate-500 text-xs">Loading meetings...</div>
            ) : meetings.length === 0 ? (
              <div className="text-center p-8 text-slate-500 text-xs">No meetings processed yet.</div>
            ) : (
              meetings.map((m) => (
                <div
                  key={m.id}
                  onClick={() => setSelectedMeeting(m)}
                  className={`p-3 rounded-lg border cursor-pointer transition-all ${
                    selectedMeeting?.id === m.id
                      ? 'bg-emerald-900/20 border-emerald-500/50 shadow-inner'
                      : 'bg-slate-900/50 border-slate-800 hover:bg-slate-800'
                  }`}
                >
                  <div className="flex justify-between items-start mb-1">
                    <h3 className="font-semibold text-white text-sm truncate pr-2">{m.title}</h3>
                    {isAdmin && (
                      <button onClick={(e) => handleDelete(e, m.id)} className="text-slate-500 hover:text-red-400 transition flex-shrink-0">
                        <Trash2 className="w-3.5 h-3.5" />
                      </button>
                    )}
                  </div>
                  <div className="flex items-center justify-between text-[11px] text-slate-400">
                    <span className="flex items-center"><Calendar className="w-3 h-3 mr-1" />{m.meetingDate}</span>
                    {m.status === 'COMPLETED' ? (
                      <span className="flex items-center text-emerald-400"><CheckCircle className="w-3 h-3 mr-1" />Processed</span>
                    ) : m.status === 'PROCESSING' ? (
                      <span className="flex items-center text-blue-400"><Clock className="w-3 h-3 mr-1" />Processing</span>
                    ) : (
                      <span className="flex items-center text-red-400"><AlertTriangle className="w-3 h-3 mr-1" />Failed</span>
                    )}
                  </div>
                </div>
              ))
            )}
          </div>
        </div>

        {/* Right Column: Meeting Detail */}
        <div className="lg:col-span-2 glass-panel rounded-xl border-slate-800 flex flex-col overflow-hidden">
          {selectedMeeting ? (
            <div className="flex flex-col h-full overflow-y-auto">
              <div className="p-6 border-b border-slate-800 bg-slate-900/50 sticky top-0 backdrop-blur-md z-10">
                <h2 className="text-xl font-bold text-white mb-2">{selectedMeeting.title}</h2>
                <div className="flex items-center space-x-4 text-xs text-slate-400">
                  <span className="flex items-center"><Calendar className="w-4 h-4 mr-1 text-emerald-400" /> {selectedMeeting.meetingDate}</span>
                  <span className="flex items-center"><Users className="w-4 h-4 mr-1 text-blue-400" /> Participants: {selectedMeeting.participants || 'Unknown'}</span>
                </div>
              </div>
              
              <div className="p-6 space-y-8">
                <section>
                  <h3 className="text-sm font-bold text-emerald-400 mb-3 uppercase tracking-wider flex items-center">
                    <FileText className="w-4 h-4 mr-2" /> Executive Summary
                  </h3>
                  <div className="text-sm text-slate-300 leading-relaxed bg-slate-900/50 p-4 rounded-lg border border-slate-800 whitespace-pre-wrap">
                    {selectedMeeting.summary || 'No summary extracted.'}
                  </div>
                </section>

                <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                  <section>
                    <h3 className="text-sm font-bold text-blue-400 mb-3 uppercase tracking-wider flex items-center">
                      <CheckCircle className="w-4 h-4 mr-2" /> Key Decisions
                    </h3>
                    <div className="text-sm text-slate-300 leading-relaxed bg-blue-900/10 p-4 rounded-lg border border-blue-900/30 min-h-[120px] whitespace-pre-wrap">
                      {selectedMeeting.decisions || 'No decisions recorded.'}
                    </div>
                  </section>
                  
                  <section>
                    <h3 className="text-sm font-bold text-amber-400 mb-3 uppercase tracking-wider flex items-center">
                      <Clock className="w-4 h-4 mr-2" /> Action Items
                    </h3>
                    <div className="text-sm text-slate-300 leading-relaxed bg-amber-900/10 p-4 rounded-lg border border-amber-900/30 min-h-[120px] whitespace-pre-wrap">
                      {selectedMeeting.actionItems || 'No action items recorded.'}
                    </div>
                  </section>
                </div>

                <section>
                  <h3 className="text-sm font-bold text-red-400 mb-3 uppercase tracking-wider flex items-center">
                    <AlertTriangle className="w-4 h-4 mr-2" /> Risks & Issues
                  </h3>
                  <div className="text-sm text-slate-300 leading-relaxed bg-red-900/10 p-4 rounded-lg border border-red-900/30 whitespace-pre-wrap">
                    {selectedMeeting.risks || 'No risks identified.'}
                  </div>
                </section>
              </div>
            </div>
          ) : (
            <div className="flex-1 flex flex-col items-center justify-center text-slate-500 p-8 text-center h-full min-h-[400px]">
              <Users className="w-16 h-16 text-slate-800 mb-4" />
              <h3 className="text-lg font-semibold text-slate-400 mb-2">No Meeting Selected</h3>
              <p className="text-xs max-w-sm">
                Select a meeting from the list on the left to view its AI-generated analysis, or upload a new transcript to begin.
              </p>
            </div>
          )}
        </div>
      </div>

      {/* Upload Modal */}
      {isUploadOpen && (
        <div className="fixed inset-0 z-50 bg-slate-950/80 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="glass-panel w-full max-w-md p-6 rounded-2xl border-slate-800 shadow-2xl space-y-4">
            <div className="flex justify-between items-center pb-3 border-b border-slate-800">
              <h3 className="font-bold text-sm text-white flex items-center space-x-2">
                <UploadCloud className="w-4 h-4 text-emerald-400" />
                <span>Upload Meeting Transcript</span>
              </h3>
              <button onClick={() => setIsUploadOpen(false)} className="text-slate-400 hover:text-white">
                <X className="w-4 h-4" />
              </button>
            </div>

            <form onSubmit={handleUploadSubmit} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-slate-300 mb-1">Select Transcript File (TXT, MD)</label>
                <input
                  type="file"
                  required
                  accept=".txt,.md,.pdf,.docx"
                  onChange={(e) => setUploadFile(e.target.files ? e.target.files[0] : null)}
                  className="w-full text-xs text-slate-400 file:mr-3 file:py-1.5 file:px-3 file:rounded-lg file:border-0 file:text-xs file:font-semibold file:bg-emerald-600/20 file:text-emerald-400 hover:file:bg-emerald-600/30"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-300 mb-1">Meeting Title</label>
                <input
                  type="text"
                  required
                  value={uploadTitle}
                  onChange={(e) => setUploadTitle(e.target.value)}
                  placeholder="e.g. Q3 Marketing Sync"
                  className="w-full bg-slate-900 border border-slate-800 rounded-lg px-3 py-2 text-xs text-white focus:outline-none focus:border-emerald-500"
                />
              </div>
              
              <div>
                <label className="block text-xs font-semibold text-slate-300 mb-1">Meeting Date</label>
                <input
                  type="date"
                  required
                  value={uploadDate}
                  onChange={(e) => setUploadDate(e.target.value)}
                  className="w-full bg-slate-900 border border-slate-800 rounded-lg px-3 py-2 text-xs text-white focus:outline-none focus:border-emerald-500"
                />
              </div>

              <button
                type="submit"
                disabled={uploading || !uploadFile}
                className="w-full py-2 px-4 bg-emerald-600 hover:bg-emerald-500 text-white font-semibold text-xs rounded-lg transition disabled:opacity-50 flex items-center justify-center"
              >
                {uploading ? (
                  <span className="flex items-center"><Clock className="w-4 h-4 mr-2 animate-spin" /> Processing Transcript...</span>
                ) : 'Analyze Meeting'}
              </button>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

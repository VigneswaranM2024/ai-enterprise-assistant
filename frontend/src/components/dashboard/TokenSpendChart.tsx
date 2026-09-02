import React from 'react';
import { TrendingUp, DollarSign } from 'lucide-react';

interface ChartDataPoint {
  day: string;
  openaiTokens: number;
  bedrockTokens: number;
  costUsd: number;
}

const WEEKLY_DATA: ChartDataPoint[] = [
  { day: 'Mon', openaiTokens: 420, bedrockTokens: 120, costUsd: 4.20 },
  { day: 'Tue', openaiTokens: 680, bedrockTokens: 210, costUsd: 6.90 },
  { day: 'Wed', openaiTokens: 950, bedrockTokens: 340, costUsd: 9.80 },
  { day: 'Thu', openaiTokens: 1100, bedrockTokens: 420, costUsd: 11.50 },
  { day: 'Fri', openaiTokens: 1420, bedrockTokens: 580, costUsd: 14.25 },
  { day: 'Sat', openaiTokens: 310, bedrockTokens: 90, costUsd: 3.10 },
  { day: 'Sun', openaiTokens: 250, bedrockTokens: 70, costUsd: 2.50 },
];

export const TokenSpendChart: React.FC = () => {
  const maxTokens = 2000;

  return (
    <div className="glass-panel p-5 rounded-xl border-slate-800 space-y-4">
      <div className="flex justify-between items-center pb-3 border-b border-slate-800">
        <div>
          <h3 className="text-xs font-bold text-white uppercase tracking-wider flex items-center space-x-2">
            <DollarSign className="w-4 h-4 text-emerald-400" />
            <span>Weekly LLM Token Expenditure</span>
          </h3>
          <p className="text-[10px] text-slate-400 mt-0.5">Comparing Azure OpenAI vs AWS Bedrock daily spend</p>
        </div>
        <div className="flex items-center space-x-3 text-[10px]">
          <span className="flex items-center space-x-1">
            <span className="w-2 h-2 rounded-full bg-indigo-500"></span>
            <span className="text-slate-400">Azure OpenAI</span>
          </span>
          <span className="flex items-center space-x-1">
            <span className="w-2 h-2 rounded-full bg-purple-500"></span>
            <span className="text-slate-400">AWS Bedrock</span>
          </span>
        </div>
      </div>

      {/* SVG Bar Chart Visualization */}
      <div className="h-48 flex items-end justify-between gap-2 pt-4 px-2">
        {WEEKLY_DATA.map((d) => {
          const openaiHeight = (d.openaiTokens / maxTokens) * 100;
          const bedrockHeight = (d.bedrockTokens / maxTokens) * 100;

          return (
            <div key={d.day} className="flex-1 flex flex-col items-center gap-2 h-full justify-end group">
              <div className="w-full max-w-[28px] flex items-end justify-center space-x-1 h-full">
                {/* Azure Bar */}
                <div
                  style={{ height: `${openaiHeight}%` }}
                  className="w-1/2 bg-indigo-500/80 group-hover:bg-indigo-400 rounded-t transition-all relative"
                  title={`Azure OpenAI: ${d.openaiTokens}k tokens`}
                ></div>
                {/* Bedrock Bar */}
                <div
                  style={{ height: `${bedrockHeight}%` }}
                  className="w-1/2 bg-purple-500/80 group-hover:bg-purple-400 rounded-t transition-all"
                  title={`AWS Bedrock: ${d.bedrockTokens}k tokens`}
                ></div>
              </div>
              <span className="text-[10px] font-medium text-slate-400 group-hover:text-white transition">
                {d.day}
              </span>
            </div>
          );
        })}
      </div>
    </div>
  );
};

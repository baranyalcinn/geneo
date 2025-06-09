import React, { useState, useEffect } from 'react';
import { 
  LineChart, Line, AreaChart, Area, BarChart, Bar, 
  PieChart, Pie, Cell, XAxis, YAxis, CartesianGrid, 
  Tooltip, Legend, ResponsiveContainer 
} from 'recharts';

interface PlayerDashboardProps {
  playerName: string;
}

interface AnalyticsData {
  accuracyOverTime: Array<{ date: string; accuracy: number; speed: number }>;
  relationshipAccuracy: Array<{ category: string; accuracy: number; total: number }>;
  dailyProgress: Array<{ date: string; gamesPlayed: number; avgScore: number }>;
  weakAreas: Array<{ area: string; needsWork: number }>;
  achievements: Array<{ id: string; name: string; unlockedAt: string }>;
  compareWithAverage: {
    playerAvg: number;
    globalAvg: number;
    familyAvg: number;
  };
}

const COLORS = ['#3B82F6', '#EF4444', '#10B981', '#F59E0B', '#8B5CF6'];

export const PlayerDashboard: React.FC<PlayerDashboardProps> = ({ playerName }) => {
  const [analytics, setAnalytics] = useState<AnalyticsData | null>(null);
  const [timeRange, setTimeRange] = useState<'week' | 'month' | 'year'>('month');
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchAnalytics();
  }, [playerName, timeRange]);

  const fetchAnalytics = async () => {
    setLoading(true);
    try {
      // API call simulation
      const response = await fetch(`/api/analytics/player/${playerName}?range=${timeRange}`);
      const data = await response.json();
      setAnalytics(data);
    } catch (error) {
      console.error('Analytics yüklenemedi:', error);
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-500"></div>
      </div>
    );
  }

  if (!analytics) {
    return (
      <div className="text-center text-gray-500 py-8">
        Analitik veriler yüklenemedi
      </div>
    );
  }

  return (
    <div className="max-w-7xl mx-auto p-6 space-y-8">
      {/* Header */}
      <div className="bg-white rounded-lg shadow-sm p-6">
        <div className="flex justify-between items-center">
          <div>
            <h1 className="text-3xl font-bold text-gray-900">{playerName}'nin Dashboard'u</h1>
            <p className="text-gray-600 mt-2">Aile ilişkileri oyunu performans analizi</p>
          </div>
          
          <div className="flex space-x-2">
            {(['week', 'month', 'year'] as const).map((range) => (
              <button
                key={range}
                onClick={() => setTimeRange(range)}
                className={`px-4 py-2 rounded-md text-sm font-medium transition-colors ${
                  timeRange === range
                    ? 'bg-blue-500 text-white'
                    : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
                }`}
              >
                {range === 'week' ? 'Hafta' : range === 'month' ? 'Ay' : 'Yıl'}
              </button>
            ))}
          </div>
        </div>
      </div>

      {/* Key Metrics */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
        <MetricCard
          title="Genel Doğruluk"
          value={`${(analytics.compareWithAverage.playerAvg * 100).toFixed(1)}%`}
          change={`+${((analytics.compareWithAverage.playerAvg - analytics.compareWithAverage.globalAvg) * 100).toFixed(1)}%`}
          changeType="increase"
          icon="📊"
        />
        
        <MetricCard
          title="Oyun Sayısı"
          value={analytics.dailyProgress.reduce((sum, day) => sum + day.gamesPlayed, 0).toString()}
          change="+12% bu ay"
          changeType="increase"
          icon="🎮"
        />
        
        <MetricCard
          title="Güçlü Alan"
          value="Doğrudan İlişkiler"
          change="95% doğruluk"
          changeType="neutral"
          icon="💪"
        />
        
        <MetricCard
          title="Gelişim Alanı"
          value="Kayın İlişkileri"
          change="Pratik önerilir"
          changeType="decrease"
          icon="📈"
        />
      </div>

      {/* Charts Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
        
        {/* Accuracy Over Time */}
        <div className="bg-white rounded-lg shadow-sm p-6">
          <h3 className="text-lg font-semibold mb-4">Zaman İçinde Gelişim</h3>
          <ResponsiveContainer width="100%" height={300}>
            <LineChart data={analytics.accuracyOverTime}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="date" />
              <YAxis domain={[0, 100]} />
              <Tooltip formatter={(value: any, name: any) => [`${value}%`, name === 'accuracy' ? 'Doğruluk' : 'Hız']} />
              <Legend />
              <Line 
                type="monotone" 
                dataKey="accuracy" 
                stroke="#3B82F6" 
                strokeWidth={2}
                name="Doğruluk %"
              />
              <Line 
                type="monotone" 
                dataKey="speed" 
                stroke="#10B981" 
                strokeWidth={2}
                name="Hız Skoru"
              />
            </LineChart>
          </ResponsiveContainer>
        </div>

        {/* Relationship Category Performance */}
        <div className="bg-white rounded-lg shadow-sm p-6">
          <h3 className="text-lg font-semibold mb-4">Kategori Bazlı Performans</h3>
          <ResponsiveContainer width="100%" height={300}>
            <BarChart data={analytics.relationshipAccuracy}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="category" angle={-45} textAnchor="end" height={80} />
              <YAxis domain={[0, 100]} />
              <Tooltip formatter={(value: any) => `${value}%`} />
              <Bar dataKey="accuracy" fill="#3B82F6" />
            </BarChart>
          </ResponsiveContainer>
        </div>

        {/* Daily Activity */}
        <div className="bg-white rounded-lg shadow-sm p-6">
          <h3 className="text-lg font-semibold mb-4">Günlük Aktivite</h3>
          <ResponsiveContainer width="100%" height={300}>
            <AreaChart data={analytics.dailyProgress}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="date" />
              <YAxis />
              <Tooltip />
              <Area 
                type="monotone" 
                dataKey="gamesPlayed" 
                stackId="1"
                stroke="#8B5CF6" 
                fill="#8B5CF6" 
                fillOpacity={0.6}
                name="Oyun Sayısı"
              />
            </AreaChart>
          </ResponsiveContainer>
        </div>

        {/* Weak Areas Focus */}
        <div className="bg-white rounded-lg shadow-sm p-6">
          <h3 className="text-lg font-semibold mb-4">Gelişim Alanları</h3>
          <ResponsiveContainer width="100%" height={300}>
            <PieChart>
              <Pie
                data={analytics.weakAreas}
                cx="50%"
                cy="50%"
                labelLine={false}
                label={({ area, needsWork }: any) => `${area} (${needsWork})`}
                outerRadius={80}
                fill="#8884d8"
                dataKey="needsWork"
              >
                {analytics.weakAreas.map((entry, index) => (
                  <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                ))}
              </Pie>
              <Tooltip />
            </PieChart>
          </ResponsiveContainer>
        </div>
      </div>

      {/* Achievements Section */}
      <div className="bg-white rounded-lg shadow-sm p-6">
        <h3 className="text-lg font-semibold mb-4">Son Başarımlar</h3>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          {analytics.achievements.slice(0, 6).map((achievement) => (
            <div key={achievement.id} className="flex items-center space-x-3 p-3 bg-gray-50 rounded-lg">
              <div className="w-10 h-10 bg-yellow-400 rounded-full flex items-center justify-center">
                🏆
              </div>
              <div>
                <p className="font-medium text-gray-900">{achievement.name}</p>
                <p className="text-sm text-gray-500">{achievement.unlockedAt}</p>
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* Comparison with Others */}
      <div className="bg-white rounded-lg shadow-sm p-6">
        <h3 className="text-lg font-semibold mb-4">Karşılaştırmalı Performans</h3>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          <div className="text-center">
            <div className="text-2xl font-bold text-blue-600">
              {(analytics.compareWithAverage.playerAvg * 100).toFixed(1)}%
            </div>
            <div className="text-gray-600">Senin Ortalamanın</div>
          </div>
          <div className="text-center">
            <div className="text-2xl font-bold text-green-600">
              {(analytics.compareWithAverage.familyAvg * 100).toFixed(1)}%
            </div>
            <div className="text-gray-600">Aile Ortalaması</div>
          </div>
          <div className="text-center">
            <div className="text-2xl font-bold text-gray-600">
              {(analytics.compareWithAverage.globalAvg * 100).toFixed(1)}%
            </div>
            <div className="text-gray-600">Genel Ortalama</div>
          </div>
        </div>
      </div>
    </div>
  );
};

interface MetricCardProps {
  title: string;
  value: string;
  change: string;
  changeType: 'increase' | 'decrease' | 'neutral';
  icon: string;
}

const MetricCard: React.FC<MetricCardProps> = ({ title, value, change, changeType, icon }) => {
  const changeColor = {
    increase: 'text-green-600',
    decrease: 'text-red-600',
    neutral: 'text-gray-600'
  }[changeType];

  return (
    <div className="bg-white rounded-lg shadow-sm p-6">
      <div className="flex items-center justify-between">
        <div>
          <p className="text-sm font-medium text-gray-600">{title}</p>
          <p className="text-2xl font-semibold text-gray-900">{value}</p>
          <p className={`text-sm ${changeColor}`}>{change}</p>
        </div>
        <div className="text-3xl">{icon}</div>
      </div>
    </div>
  );
}; 
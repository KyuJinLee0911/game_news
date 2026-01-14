'use client';

import { GameNews } from '@/types/news';
import NewsCard from './NewsCard';
import { useState } from 'react';

interface NewsListProps {
  initialNews: GameNews[];
}

export default function NewsList({ initialNews }: NewsListProps) {
  const [news, setNews] = useState<GameNews[]>(initialNews);
  const [loading, setLoading] = useState(false);
  const [copying, setCopying] = useState(false);
  const [copySuccess, setCopySuccess] = useState(false);
  const [lastUpdate, setLastUpdate] = useState<Date>(new Date());

  const handleRefresh = async () => {
    setLoading(true);
    try {
      const response = await fetch('/api/news');
      if (response.ok) {
        const data = await response.json();
        setNews(data);
        setLastUpdate(new Date());
      }
    } catch (error) {
      console.error('Failed to refresh news:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleCopyToClipboard = async () => {
    setCopying(true);
    setCopySuccess(false);
    try {
      const response = await fetch('/api/news/share-text');
      if (response.ok) {
        const text = await response.text();
        await navigator.clipboard.writeText(text);
        setCopySuccess(true);
        setTimeout(() => setCopySuccess(false), 3000);
      }
    } catch (error) {
      console.error('Failed to copy to clipboard:', error);
      alert('클립보드 복사에 실패했습니다.');
    } finally {
      setCopying(false);
    }
  };

  return (
    <div className="w-full">
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-3 mb-6">
        <div className="text-sm text-gray-500 dark:text-gray-400">
          마지막 업데이트: {lastUpdate.toLocaleTimeString('ko-KR')}
        </div>
        <div className="flex gap-2 w-full sm:w-auto">
          <button
            onClick={handleCopyToClipboard}
            disabled={copying}
            className="flex-1 sm:flex-none px-4 py-2 bg-green-600 hover:bg-green-700 disabled:bg-gray-400 text-white rounded-lg transition-colors duration-200 text-sm font-medium"
          >
            {copying ? '복사 중...' : copySuccess ? '복사 완료!' : '공유 텍스트 복사'}
          </button>
          <button
            onClick={handleRefresh}
            disabled={loading}
            className="flex-1 sm:flex-none px-4 py-2 bg-blue-600 hover:bg-blue-700 disabled:bg-gray-400 text-white rounded-lg transition-colors duration-200 text-sm font-medium"
          >
            {loading ? '새로고침 중...' : '새로고침'}
          </button>
        </div>
      </div>

      {news.length === 0 ? (
        <div className="text-center py-12 text-gray-500 dark:text-gray-400">
          뉴스를 불러올 수 없습니다.
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {news.map((item, index) => (
            <NewsCard key={index} news={item} index={index} />
          ))}
        </div>
      )}
    </div>
  );
}

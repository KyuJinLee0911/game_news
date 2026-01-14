import { GameNews } from '@/types/news';

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';

export async function fetchGameNews(): Promise<GameNews[]> {
  try {
    const response = await fetch(`${API_BASE_URL}/api/news`, {
      cache: 'no-store',
      headers: {
        'Content-Type': 'application/json',
      },
    });

    if (!response.ok) {
      throw new Error(`Failed to fetch news: ${response.status}`);
    }

    return await response.json();
  } catch (error) {
    console.error('Error fetching game news:', error);
    return [];
  }
}

export async function fetchShareText(): Promise<string> {
  try {
    const response = await fetch(`${API_BASE_URL}/api/news/share-text`, {
      cache: 'no-store',
      headers: {
        'Content-Type': 'text/plain',
      },
    });

    if (!response.ok) {
      throw new Error(`Failed to fetch share text: ${response.status}`);
    }

    return await response.text();
  } catch (error) {
    console.error('Error fetching share text:', error);
    throw error;
  }
}

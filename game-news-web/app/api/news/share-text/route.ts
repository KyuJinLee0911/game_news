import { NextResponse } from 'next/server';

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';

export async function GET() {
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

    const text = await response.text();
    return new NextResponse(text, {
      headers: {
        'Content-Type': 'text/plain; charset=utf-8',
      },
    });
  } catch (error) {
    console.error('Error fetching share text:', error);
    return NextResponse.json(
      { error: 'Failed to fetch share text' },
      { status: 500 }
    );
  }
}

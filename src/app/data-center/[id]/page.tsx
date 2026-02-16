'use client';

import { use } from 'react';
import MainLayout from '@/components/layout/MainLayout';
import DataCenterDashboard from '@/components/data-center/DataCenterDashboard';

export default function DataCenterDetailPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);
  const questionId = Number(id);

  return (
    <MainLayout>
      <DataCenterDashboard questionId={questionId} />
    </MainLayout>
  );
}

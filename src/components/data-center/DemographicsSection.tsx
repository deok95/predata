'use client';

import { Globe, Briefcase, Calendar } from 'lucide-react';
import DemographicsPieChart from './DemographicsPieChart';
import type { VoteDemographicsReport } from '@/types/api';

interface DemographicsSectionProps {
  demographics: VoteDemographicsReport;
  isDark: boolean;
}

export default function DemographicsSection({ demographics, isDark }: DemographicsSectionProps) {
  const countryData = demographics.byCountry.map(c => ({
    name: c.countryCode,
    value: c.total,
    yesPercentage: c.yesPercentage,
  }));

  const jobData = demographics.byJob.map(j => ({
    name: j.jobCategory,
    value: j.total,
    yesPercentage: j.yesPercentage,
  }));

  const ageData = demographics.byAge.map(a => ({
    name: `${a.ageGroup}대`,
    value: a.total,
    yesPercentage: a.yesPercentage,
  }));

  return (
    <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
      <DemographicsPieChart
        title="국가별 분포"
        icon={Globe}
        iconColor="text-blue-500"
        data={countryData}
        isDark={isDark}
      />
      <DemographicsPieChart
        title="직업별 분포"
        icon={Briefcase}
        iconColor="text-purple-500"
        data={jobData}
        isDark={isDark}
      />
      <DemographicsPieChart
        title="연령별 분포"
        icon={Calendar}
        iconColor="text-amber-500"
        data={ageData}
        isDark={isDark}
      />
    </div>
  );
}

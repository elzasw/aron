import React, { useEffect, useState, useContext, ComponentType } from 'react';
import { noop } from 'lodash';
import { useCrudSource } from 'utils/crud-source-hook';
import { useEventCallback } from 'utils/event-callback-hook';
import { Report } from 'modules/reporting/reporting-types';
import { ReportingContext } from 'modules/reporting/reporting-context';
import { DashboardCardProps, CardProps } from './dashboard-types';
import { CardUniversal } from './cards/universal/card-universal';
import { CardAdd } from './cards/add/card-add';
import { DashboardContext } from './dashboard-context';

export function DashboardCard({ id }: DashboardCardProps) {
  const { cardFactory } = useContext(DashboardContext);
  const { url } = useContext(ReportingContext);

  const [report, setReport] = useState<Report>();

  const source = useCrudSource<Report>({
    url,
    handleGetError: noop,
  });

  const load = useEventCallback(async () => {
    if (id !== 'ADD') {
      const report = await source.get(id);
      setReport(report ?? undefined);
    }
  });

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id]);

  const CardComponent = combinedCardFactory(id, cardFactory);

  return (
    <CardComponent
      report={report}
      load={load}
      loading={source.loading}
      definitionId={id}
    />
  );
}

function combinedCardFactory(
  definitionId: string,
  cardFactory: (definitionId: string) => ComponentType<CardProps> | undefined
) {
  if (definitionId === 'ADD') {
    return CardAdd;
  } else {
    return cardFactory(definitionId) ?? CardUniversal;
  }
}

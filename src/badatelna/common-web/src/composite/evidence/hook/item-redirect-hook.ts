import { useContext, useLayoutEffect } from 'react';
import { useParams, useLocation } from 'react-router-dom';
import { NavigationContext } from 'composite/navigation/navigation-context';
import { EvidenceStateAction } from '../evidence-types';

export function useEvidenceItemRedirect() {
  const { id } = useParams();
  const pathname: string = useLocation().pathname;

  const { navigate } = useContext(NavigationContext);

  const base = pathname.substr(0, pathname.lastIndexOf('/'));

  useLayoutEffect(() => {
    if (id !== undefined) {
      navigate(base, true, {
        action: EvidenceStateAction.SHOW_ITEM,
        data: id,
      });
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);
}

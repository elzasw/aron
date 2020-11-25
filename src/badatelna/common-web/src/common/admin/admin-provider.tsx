import * as React from 'react';
import { Switch, Route } from 'react-router-dom';
import { FormattedMessage } from 'react-intl';
import { MenubarContext } from 'composite/menubar/menubar-context';
import { MenuItem } from 'composite/menubar/menu/menu-types';
import { NavigationContext } from 'composite/navigation/navigation-context';
import { AdminProviderProps } from './admin-provider-types';
import { AdminReindex } from './admin-reindex';
import { UserContext } from 'common/user/user-context';

export function AdminProvider({ permission, prefix }: AdminProviderProps) {
  const { modifyItems } = React.useContext(MenubarContext);
  const { navigate } = React.useContext(NavigationContext);
  const { hasPermission } = React.useContext(UserContext);

  const menuItems: MenuItem[] = React.useMemo(
    () => [
      {
        label: (
          <FormattedMessage
            id="EAS_MENU_ITEM_DEVTOOLS"
            defaultMessage="Vývojové nástroje"
          />
        ),
        items: [
          {
            label: (
              <FormattedMessage
                id="EAS_MENU_ITEM_REINDEX"
                defaultMessage="Reindex"
              />
            ),
            onClick: () => {
              navigate(`${prefix}/admin/reindex`);
            },
          },
        ],
      },
    ],
    [navigate, prefix]
  );

  const show = hasPermission(permission);

  // adds items to menu
  React.useEffect(() => {
    if (show) {
      modifyItems((items) => [...items, ...menuItems]);
    }

    return () => {
      if (show) {
        modifyItems((items) =>
          items.filter((item) => !menuItems.includes(item))
        );
      }
    };
  }, [menuItems, modifyItems, show]);

  return (
    <Switch>
      {show && (
        <Route path={`${prefix}/admin/reindex`} component={AdminReindex} />
      )}
    </Switch>
  );
}

import * as React from 'react';
import { Switch, Route } from 'react-router-dom';
import { FormattedMessage } from 'react-intl';
import { MenubarContext } from 'composite/menubar/menubar-context';
import { MenuItem } from 'composite/menubar/menu/menu-types';
import { NavigationContext } from 'composite/navigation/navigation-context';
import { AdminProviderProps } from './admin-provider-types';
import { AdminReindex } from './admin-reindex';
import { UserContext } from 'common/user/user-context';
import { AdminContext } from './admin-context';
import { AdminConsole } from './console/admin-console';
import { SoapMessages } from './soap/logger/message/messages';
import { compact } from 'lodash';
import { WebsocketContext } from 'common/web-socket/web-socket-context';

export function AdminProvider({
  permission,
  prefix,
  reindexUrl,
  soapMessagesUrl,
}: AdminProviderProps) {
  const { modifyItems } = React.useContext(MenubarContext);
  const { navigate } = React.useContext(NavigationContext);
  const { hasPermission } = React.useContext(UserContext);
  const { isWsActive } = React.useContext(WebsocketContext);

  const menuItems: MenuItem[] = React.useMemo(
    () => [
      {
        label: (
          <FormattedMessage
            id="EAS_MENU_ITEM_DEVTOOLS"
            defaultMessage="Vývojové nástroje"
          />
        ),
        isActive: (pathname) => {
          return [
            `${prefix}/admin/reindex`,
            `${prefix}/admin/console`,
            `${prefix}/admin/soap/logger/messages`,
          ].includes(pathname);
        },
        items: compact([
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
          isWsActive
            ? {
                label: (
                  <FormattedMessage
                    id="EAS_MENU_ITEM_CONSOLE"
                    defaultMessage="Konzole"
                  />
                ),
                onClick: () => {
                  navigate(`${prefix}/admin/console`);
                },
              }
            : undefined,
          soapMessagesUrl !== undefined
            ? {
                label: (
                  <FormattedMessage
                    id="EAS_MENU_ITEM_SOAP_MESSAGES"
                    defaultMessage="SOAP komunikace"
                  />
                ),
                onClick: () => {
                  navigate(`${prefix}/admin/soap/logger/messages`);
                },
              }
            : undefined,
        ]),
      },
    ],
    [isWsActive, navigate, prefix, soapMessagesUrl]
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
    <AdminContext.Provider
      value={{
        reindexUrl: reindexUrl!,
        soapMessagesUrl: soapMessagesUrl!,
      }}
    >
      <Switch>
        {show && reindexUrl !== undefined && (
          <Route path={`${prefix}/admin/reindex`} component={AdminReindex} />
        )}
        {show && isWsActive && (
          <Route path={`${prefix}/admin/console`} component={AdminConsole} />
        )}
        {show && soapMessagesUrl !== undefined && (
          <Route
            path={`${prefix}/admin/soap/logger/messages`}
            component={SoapMessages}
          />
        )}
      </Switch>
    </AdminContext.Provider>
  );
}

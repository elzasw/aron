import React, { useState, useEffect, useMemo, useCallback } from 'react';
import { useParams, Link } from 'react-router-dom';
import { get, find, flatten, compact, isEmpty, sortBy } from 'lodash';
import classNames from 'classnames';
import DoubleArrowIcon from '@material-ui/icons/DoubleArrow';
import AccountTreeIcon from '@material-ui/icons/AccountTree';
import { useIntl } from 'react-intl';

import {
  ApiUrl,
  ApuType,
  ModulePath,
  ApuPartItemDataType,
  navigationItems,
  ApuPartItemEnum,
  Message,
} from '../../enums';
import { useStyles } from './styles';
import { useLayoutStyles, useSpacingStyles } from '../../styles';
import {
  useGet,
  getApu,
  sortByArray,
  useEvidenceNavigation,
} from '../../common-utils';
import { DetailProps } from './types';
import {
  ApuEntity,
  ApuPartType,
  ApuPartItemType,
  ApuPartItem,
} from '../../types';
import {
  findApuParts,
  filterApuPartTypes,
  getRelatedApusFilter,
  getParentBreadcrumbs,
} from './utils';
import { EvidenceDetailDao } from './evidence-detail-dao';
import { EvidenceDetailTree } from './evidence-detail-tree';
import { getPathByType, useAppState } from '../../common-utils';
import { Module, Loading, Button } from '../../components';
import { EvidenceDetailItem } from './evidence-detail-item';
import { EvidenceDetailAttachments } from './evidence-detail-attachments';

export function EvidenceDetail({
  apuPartTypes,
  apuPartItemTypes,
}: DetailProps) {
  const classes = useStyles();
  const layoutClasses = useLayoutStyles();
  const spacingClasses = useSpacingStyles();

  const { appState, updateAppState } = useAppState();

  const { evidencePath } = appState;

  const navigateTo = useEvidenceNavigation();

  const [open, setOpen] = useState(false);

  const [loadingBasic, setLoading] = useState(false);

  const [apus, setApus] = useState<ApuEntity[]>([]);

  const [archdescRootRefLoading, setArchdescRootRefLoading] = useState(false);

  const [archdescRootRef, setArchdescRootRef] = useState<ApuPartItem | null>(
    null
  );

  const [archdescRootRefItemId, setArchdescRootRefItemId] = useState<string>();

  const { id } = useParams();

  const url = `${ApiUrl.APU}/${id}`;

  const [item, loadingItem] = useGet<ApuEntity>(url);

  const loading = loadingItem || loadingBasic;

  const findRoot = useCallback((root?: ApuEntity): ApuEntity | undefined => {
    return root?.parent ? findRoot(root.parent) : root;
  }, []);

  const [root, setRoot] = useState<ApuEntity | undefined>(findRoot(item));

  const { formatMessage } = useIntl();

  useEffect(() => {
    const rootItem = findRoot(item);
    if (rootItem && rootItem.id !== root?.id) {
      setRoot(rootItem);
    }
  }, [item, root, findRoot]);

  useEffect(() => {
    if (
      item &&
      item.parts &&
      !archdescRootRefLoading &&
      (!archdescRootRefItemId || item.id !== archdescRootRefItemId)
    ) {
      setArchdescRootRefItemId(item.id);

      let newArchdescRootRef: ApuPartItem | null = null;

      item.parts!.some((part) =>
        part.items.some((item: ApuPartItem) => {
          if (item.type === ApuPartItemEnum.ARCHDESC_ROOT_REF) {
            newArchdescRootRef = item;
            return true;
          }

          return false;
        })
      );

      if (
        newArchdescRootRef &&
        (!archdescRootRef || newArchdescRootRef!.id !== archdescRootRef.id)
      ) {
        const load = async () => {
          setArchdescRootRefLoading(true);

          setArchdescRootRef(
            (await getApu(newArchdescRootRef!.value))
              ? newArchdescRootRef
              : null
          );

          setArchdescRootRefLoading(false);
        };

        load();
      }
    }
  }, [
    item,
    apuPartTypes,
    apuPartItemTypes,
    archdescRootRef,
    archdescRootRefLoading,
    archdescRootRefItemId,
  ]);

  const items: (ApuPartType & {
    items: (ApuPartItem & {
      code: string;
      name: string;
      type: ApuPartItemDataType;
    })[];
  })[] = useMemo(
    () =>
      item && item.parts
        ? sortByArray(
            flatten(
              item.parts.map((part) =>
                filterApuPartTypes(apuPartTypes, [part]).map(
                  (apuPartType: ApuPartType) => {
                    return {
                      ...apuPartType,
                      items: sortByArray(
                        flatten(
                          filterApuPartTypes(apuPartItemTypes, part.items).map(
                            ({ code, name, type }: ApuPartItemType) => {
                              return compact(
                                findApuParts(part.items, code).map((item) => {
                                  return item.type ===
                                    ApuPartItemEnum.ARCHDESC_ROOT_REF
                                    ? null
                                    : {
                                        ...item,
                                        code,
                                        name,
                                        type,
                                      };
                                })
                              );
                            }
                          )
                        ),
                        apuPartItemTypes,
                        'code'
                      ),
                    };
                  }
                )
              )
            ),
            apuPartTypes,
            'code'
          )
        : [],
    [item, apuPartTypes, apuPartItemTypes]
  );

  useEffect(() => {
    if (items && items.length) {
      const filteredItems: string[] = [];

      items.forEach(
        ({ items }) =>
          items &&
          items.forEach(
            (item) =>
              item.type === ApuPartItemDataType.APU_REF &&
              filteredItems.push(item.value)
          )
      );

      if (filteredItems.length) {
        const load = async () => {
          setLoading(true);

          const promisses = filteredItems.map(getApu);

          let result: ApuEntity[] = [];
          try {
            result = compact(await Promise.all(promisses));
          } catch (error) {
            console.log(error);
            result = [];
          }

          setLoading(false);

          setApus(result);
        };

        load();
      }
    }
  }, [items]);

  const type = item?.type;

  const icon =
    type === ApuType.ENTITY
      ? 'fas fa-key'
      : type === ApuType.FUND
      ? 'fas fa-sitemap'
      : type === ApuType.FINDING_AID
      ? 'fas fa-book-reader'
      : null;

  const path = type ? getPathByType(type) : undefined;

  useEffect(() => {
    if (evidencePath && path) {
      updateAppState({ evidencePath: null });
    }
  }, [evidencePath, path, updateAppState]);

  return (
    <Module
      {...{
        items: [
          {
            path: evidencePath || path,
            label:
              find(
                navigationItems,
                (item) => item.path === (evidencePath || path)
              )?.label || '',
          },
          ...(item ? getParentBreadcrumbs(item.parent) : []),
          {
            label: item ? item.name : '...',
          },
        ],
      }}
    >
      <div className={classes.evidenceDetail}>
        <Loading {...{ loading }} />
        <div className={spacingClasses.paddingBig}>
          {item && path === ModulePath.ARCH_DESC && root ? (
            <>
              <EvidenceDetailTree {...{ item, id: root.id }} />
              <div className={spacingClasses.paddingBottom} />
            </>
          ) : (
            <></>
          )}
          {item ? (
            <div
              className={classNames(
                classes.evidenceDetailTop,
                layoutClasses.flexSpaceBetweenBottom,
                spacingClasses.marginBottom
              )}
            >
              <div className={layoutClasses.flex}>
                <div
                  className={classNames(
                    layoutClasses.flexColumnCenter,
                    spacingClasses.marginRightBig,
                    spacingClasses.marginBottomSmall
                  )}
                >
                  {icon && (
                    <i
                      className={classNames(
                        icon,
                        classes.evidenceDetailIcon,
                        spacingClasses.marginBottomSmall
                      )}
                    />
                  )}
                  <Button
                    className={classes.findRelatedButton}
                    label={formatMessage({ id: Message.FIND_RELATED })}
                    outlined={true}
                    size="small"
                    onClick={() => {
                      navigateTo(
                        ModulePath.APU,
                        1,
                        10,
                        '',
                        getRelatedApusFilter(id, item.name)
                      );
                    }}
                  />
                </div>
                <div className={spacingClasses.paddingBottomSmall}>
                  <h3 className={spacingClasses.marginBottomSmall}>
                    {item.name}
                  </h3>
                  {item.description ? (
                    path === ModulePath.ARCH_DESC ||
                    path === ModulePath.ENTITY ? (
                      <h3
                        className={classNames(
                          classes.evidenceDetailDescription,
                          spacingClasses.marginBottomSmall
                        )}
                      >
                        {item.description}
                      </h3>
                    ) : (
                      <h4
                        className={classNames(
                          classes.evidenceDetailDescription,
                          spacingClasses.marginBottomSmall
                        )}
                      >
                        {item.description}
                      </h4>
                    )
                  ) : (
                    <></>
                  )}
                  {archdescRootRef ? (
                    <Link
                      to={{
                        pathname: `${ModulePath.APU}/${archdescRootRef.value}`,
                      }}
                      className={classNames(
                        classes.link,
                        layoutClasses.flexAlignCenter,
                        spacingClasses.marginTop
                      )}
                    >
                      <AccountTreeIcon
                        className={spacingClasses.marginRightSmall}
                      />
                      {get(
                        find(
                          apuPartItemTypes,
                          ({ code }) =>
                            code === ApuPartItemEnum.ARCHDESC_ROOT_REF
                        ),
                        'name',
                        formatMessage({
                          id: Message.TREE_VIEW_CURRENT_STATE,
                        })
                      )}
                    </Link>
                  ) : (
                    <></>
                  )}
                </div>
              </div>
              <DoubleArrowIcon
                className={classNames(
                  classes.evidenceDetailTopIcon,
                  open && classes.evidenceDetailTopIconOpen,
                  spacingClasses.marginBottom
                )}
                onClick={() => setOpen(!open)}
              />
            </div>
          ) : (
            <></>
          )}
          {items
            .map(({ items, ...item }) => ({
              ...item,
              items: items.filter(({ visible, value }) => visible && value),
            }))
            .filter(({ items }) => !isEmpty(items))
            .map((item, index) => (
              <EvidenceDetailItem
                {...{
                  key: `${item.name}-${index}`,
                  ...item,
                  index,
                  open,
                  apus,
                }}
              />
            ))}
          {item && (
            <EvidenceDetailAttachments
              items={sortBy(item.attachments, 'order')}
              setLoading={setLoading}
            />
          )}
          {item && (
            <EvidenceDetailDao items={sortBy(item.digitalObjects, 'order')} />
          )}
        </div>
      </div>
    </Module>
  );
}

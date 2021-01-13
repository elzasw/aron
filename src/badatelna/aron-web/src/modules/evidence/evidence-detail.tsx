import React, {
  useState,
  useEffect,
  useContext,
  useMemo,
  useRef,
  useCallback,
} from 'react';
import { useParams } from 'react-router-dom';
import { get, find, flatten, reverse, compact, isEmpty } from 'lodash';
import classNames from 'classnames';
import LinearProgress from '@material-ui/core/LinearProgress';
import Tooltip from '@material-ui/core/Tooltip';
import DoubleArrowIcon from '@material-ui/icons/DoubleArrow';
import PersonIcon from '@material-ui/icons/Person';
import InsertPhotoIcon from '@material-ui/icons/InsertPhoto';
import AccountTreeIcon from '@material-ui/icons/AccountTree';
import { FormattedMessage } from 'react-intl';

import { NavigationContext } from '@eas/common-web';

import {
  ApiUrl,
  ApuType,
  ApuPartViewType,
  ModulePath,
  ApuPartItemDataType,
  navigationItems,
  ApuPartItemEnum,
  Message,
} from '../../enums';
import { useStyles } from './styles';
import { useLayoutStyles, useSpacingStyles } from '../../styles';
import { useGet, usePrevious, getApu, sortByArray } from '../../common-utils';
import { DetailProps } from './types';
import {
  ApuEntity,
  ApuPartType,
  ApuPartItemType,
  ApuPartItem,
} from '../../types';
import { findApuParts, filterApuPartTypes, getRelatedApusURL } from './utils';
import { EvidenceDetailDao } from './evidence-detail-dao';
import { EvidenceDetailTree } from './evidence-detail-tree';
import {
  getPathByType,
  openInNewTab,
  formatUnitDate,
} from '../../common-utils';
import { Module } from '../../components';

function ItemValue({
  value,
  type,
  apus,
}: {
  value: string;
  type: ApuPartItemDataType;
  apus?: ApuEntity[];
}) {
  const classes = useStyles();

  const { navigate } = useContext(NavigationContext);

  let result: string | JSX.Element = value;

  switch (type) {
    case ApuPartItemDataType.UNITDATE:
      result = formatUnitDate(value);
      break;
    case ApuPartItemDataType.APU_REF:
      result = (
        <span
          className={classes.link}
          onClick={() => navigate(`${ModulePath.APU}/${value}`)}
        >
          {get(
            find(apus, ({ id }) => id === value),
            'name',
            'Zobrazit APU'
          )}
        </span>
      );
      break;
    case ApuPartItemDataType.LINK:
      result = (
        <span className={classes.link} onClick={() => openInNewTab(value)}>
          {value}
        </span>
      );
      break;
  }

  return <span>{result}</span>;
}

function Component({
  name,
  viewType,
  items,
  open: outterOpen,
  index,
  apus,
}: {
  name: string;
  viewType: ApuPartViewType;
  items: any[];
  open: boolean;
  index: number;
  apus: ApuEntity[];
}) {
  const classes = useStyles();
  const layoutClasses = useLayoutStyles();
  const spacingClasses = useSpacingStyles();

  const [open, setOpen] = useState(viewType === ApuPartViewType.STANDALONE);

  const previousOutterOpen = usePrevious(outterOpen);

  useEffect(() => {
    if (
      viewType === ApuPartViewType.GROUPED &&
      previousOutterOpen !== undefined &&
      previousOutterOpen !== outterOpen
    ) {
      setOpen(outterOpen);
    }
  }, [viewType, outterOpen, previousOutterOpen]);

  const labelClassName = classNames(
    classes.evidenceDetailItemText,
    classes.evidenceDetailItemLabel,
    spacingClasses.marginRight,
    spacingClasses.paddingRight
  );

  return (
    <div
      className={classNames(
        classes.evidenceDetailItem,
        index && classes.evidenceDetailItemNotFirst
      )}
    >
      <div className={layoutClasses.flex}>
        <div
          className={classNames(labelClassName, spacingClasses.paddingTopSmall)}
        >
          {name}
        </div>
        {viewType === ApuPartViewType.GROUPED ? (
          <Tooltip
            title={
              <FormattedMessage
                id={open ? Message.CLICK_TO_COLLAPSE : Message.CLICK_TO_EXPAND}
              />
            }
          >
            <div
              className={classNames(
                classes.evidenceDetailItemText,
                classes.bold,
                spacingClasses.paddingTopSmall
              )}
              style={{ cursor: 'pointer' }}
              onClick={() => setOpen(!open)}
            >
              {items
                .filter(({ type }) => type !== ApuPartItemDataType.APU_REF)
                .map(({ value, type }, i) => (
                  <React.Fragment key={`${value}-${i}`}>
                    <ItemValue {...{ value, type }} />{' '}
                  </React.Fragment>
                ))}
            </div>
          </Tooltip>
        ) : (
          <div />
        )}
      </div>
      {open ? (
        items.map(({ name, value, type }, i) =>
          type !== ApuPartItemDataType.APU_REF ||
          find(apus, ({ id }) => id === value) ? (
            <div key={`${name}-${i}`} className={layoutClasses.flex}>
              <div className={labelClassName}>{name}</div>
              <div className={classes.evidenceDetailItemText}>
                <ItemValue {...{ value, type, apus }} />
              </div>
            </div>
          ) : (
            <div key={`${name}-${i}`} />
          )
        )
      ) : (
        <></>
      )}
    </div>
  );
}

const getParentBreadcrumbs = (parent?: ApuEntity) => {
  const breadcrumbs = [];

  let current = parent;

  while (current) {
    breadcrumbs.push({
      path: `${ModulePath.APU}/${current.id}`,
      label: current.name,
    });

    current = current.parent;
  }

  return reverse(breadcrumbs);
};

export function EvidenceDetail({
  apuPartTypes,
  apuPartItemTypes,
}: DetailProps) {
  const classes = useStyles();
  const layoutClasses = useLayoutStyles();
  const spacingClasses = useSpacingStyles();

  const [open, setOpen] = useState(false);

  const [loadingItems, setLoadingItems] = useState(false);

  const [apus, setApus] = useState<ApuEntity[]>([]);

  const { id } = useParams();

  const url = `${ApiUrl.APU}/${id}`;

  const [item, loading] = useGet<ApuEntity>(url);

  const { navigate } = useContext(NavigationContext);

  const archdescRootRef = useRef<ApuPartItem | null>(null);

  const findRoot = useCallback((root?: ApuEntity): ApuEntity | undefined => {
    return root?.parent ? findRoot(root.parent) : root;
  }, []);

  const [root, setRoot] = useState<ApuEntity | undefined>(findRoot(item));

  useEffect(() => {
    const rootItem = findRoot(item);
    if (rootItem && rootItem.id !== root?.id) {
      setRoot(rootItem);
    }
  }, [item, root, findRoot]);

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
                                  const isArchdescRootRef =
                                    item.type ===
                                    ApuPartItemEnum.ARCHDESC_ROOT_REF;

                                  if (isArchdescRootRef) {
                                    archdescRootRef.current = item;
                                  }

                                  return isArchdescRootRef
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
          setLoadingItems(true);

          const promisses = filteredItems.map(getApu);

          let result: ApuEntity[] = [];
          try {
            result = compact(await Promise.all(promisses));
          } catch (error) {
            console.log(error);
            result = [];
          }

          setLoadingItems(false);

          setApus(result);
        };

        load();
      }
    }
  }, [items]);

  if (!item || loadingItems) {
    return loading || loadingItems ? <LinearProgress /> : <></>;
  }

  const { name, description, type, digitalObjects, parent } = item;

  const ImageComponent = type === ApuType.ENTITY ? PersonIcon : InsertPhotoIcon;

  const path = getPathByType(type);

  return (
    <Module
      {...{
        items: [
          {
            path,
            label:
              find(navigationItems, (item) => item.path === path)?.label || '',
          },
          ...getParentBreadcrumbs(parent),
          {
            label: name,
          },
        ],
        toolbar: (
          <div
            className={classNames(
              classes.findRelatedButton,
              layoutClasses.flexCentered,
              spacingClasses.paddingHorizontal
            )}
            onClick={() => navigate(getRelatedApusURL(name, id))}
          >
            <FormattedMessage id={Message.FIND_RELATED} />
          </div>
        ),
      }}
    >
      <div className={classes.evidenceDetail}>
        <div className={spacingClasses.paddingBig}>
          {path === ModulePath.ARCH_DESC ? (
            <h3>{name}</h3>
          ) : (
            <div
              className={classNames(
                classes.evidenceDetailTop,
                layoutClasses.flexSpaceBetweenBottom,
                spacingClasses.marginBottom
              )}
            >
              <div className={layoutClasses.flex}>
                <ImageComponent
                  className={classNames(
                    type === ApuType.ENTITY
                      ? classes.evidenceDetailImage
                      : classes.evidenceDetailImageBig,
                    spacingClasses.marginRightBig
                  )}
                />
                <div className={spacingClasses.paddingBottomSmall}>
                  <h3 className={spacingClasses.marginBottomSmall}>{name}</h3>
                  <h4 className={spacingClasses.marginBottomSmall}>
                    {description}
                  </h4>
                  {archdescRootRef.current ? (
                    <div
                      className={classNames(
                        classes.link,
                        layoutClasses.flexAlignCenter,
                        spacingClasses.marginTop
                      )}
                      onClick={() =>
                        navigate(
                          `${ModulePath.APU}/${archdescRootRef.current!.value}`
                        )
                      }
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
                        'Pohled na celý AS (aktuální stav)'
                      )}
                    </div>
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
          )}
          {path === ModulePath.ARCH_DESC && root ? (
            <EvidenceDetailTree {...{ item, id: root.id }} />
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
              <Component
                {...{
                  key: `${item.name}-${index}`,
                  ...item,
                  index,
                  open,
                  apus,
                }}
              />
            ))}
          <EvidenceDetailDao items={digitalObjects || []} />
        </div>
      </div>
    </Module>
  );
}

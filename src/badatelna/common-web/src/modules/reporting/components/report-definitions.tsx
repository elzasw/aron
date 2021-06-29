import React from 'react';
import { useIntl } from 'react-intl';
import TreeView from '@material-ui/lab/TreeView';
import TreeItem from '@material-ui/lab/TreeItem';
import ExpandMoreIcon from '@material-ui/icons/ExpandMore';
import ChevronRightIcon from '@material-ui/icons/ChevronRight';
import { useEventCallback } from 'utils/event-callback-hook';
import { Panel } from 'components/panel/panel';
import { useStyles } from '../reporting-styles';
import { useReportDefinitionsLogic } from '../hook/definitions-hook';
import { ReportDefinition } from '../reporting-types';

export function ReportDefinitions({
  selected,
  onSelect,
}: {
  selected: ReportDefinition | undefined;
  onSelect: (attribute: ReportDefinition | undefined) => void;
}) {
  const intl = useIntl();
  const classes = useStyles();

  const { definitions } = useReportDefinitionsLogic();

  const handleSelect = useEventCallback((e: any, value: string) => {
    if (value.startsWith('GROUP_')) {
      return;
    }

    const definition = definitions.find(
      (definition) => definition.id === value
    );

    onSelect(definition);
  });

  const groups = makeHierarchy(definitions);

  return (
    <div className={classes.wrapperDefinitions}>
      <Panel
        label={intl.formatMessage({
          id: 'EAS_REPORTING_DEFINITIONS_PANEL_TITLE',
          defaultMessage: 'Výběr reportu',
        })}
        sideBorder={true}
        className={classes.panel}
        fitHeight
      >
        {groups.length > 0 && (
          <TreeView
            selected={selected?.id ?? ''}
            onNodeSelect={handleSelect}
            defaultCollapseIcon={<ExpandMoreIcon />}
            defaultExpandIcon={<ChevronRightIcon />}
            defaultExpanded={groups.map((g) => 'GROUP_' + g.id)}
          >
            {groups.map((group) => (
              <TreeItem
                key={group.id}
                nodeId={'GROUP_' + group.id}
                label={group.label}
              >
                {group.definitions.map((definition) => (
                  <TreeItem
                    key={definition.id}
                    nodeId={definition.id}
                    label={definition.label}
                  />
                ))}
              </TreeItem>
            ))}
          </TreeView>
        )}
      </Panel>
    </div>
  );
}

export interface ReportDefinitionModel {
  label: string;
  id: string;
}

export interface ReportGroupModel {
  label: string;
  id: string;

  definitions: ReportDefinitionModel[];
}

function makeHierarchy(items: ReportDefinition[]) {
  const groups: ReportGroupModel[] = [];

  items.forEach((item) => {
    let group = groups.find((g) => g.id === item.groupId);

    if (group === undefined) {
      group = {
        id: item.groupId,
        label: item.groupLabel,
        definitions: [],
      };
      groups.push(group);
    }

    group.definitions.push({
      id: item.id,
      label: item.label,
    });
  });

  return groups;
}

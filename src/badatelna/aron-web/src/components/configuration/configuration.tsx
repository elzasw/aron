import React, { FC, useContext, createContext } from 'react';
import { AppHeaderItemCode } from '../../enums';

export interface ConfigurationType {
    allowDetailExpand?: boolean,
    alternativeItemLabel?: boolean;
    compactAppHeader?: boolean;
    showAppLogo?: boolean,
    showHeader?: boolean;
    showShareButtons?: boolean;
    showStandalonePartName?: boolean;
    showMainPageBreadcrumb?: boolean;
    headerItems?: Array<AppHeaderItemCode>;
}


const defaultConfiguration:ConfigurationType = {
    allowDetailExpand: true,
    alternativeItemLabel: false,
    compactAppHeader: false,
    showAppLogo: true,
    showHeader: true,
    showShareButtons: false,
    showStandalonePartName: true,
    showMainPageBreadcrumb: true,
    headerItems: [
        AppHeaderItemCode.FUND,
        AppHeaderItemCode.ARCH_DESC,
        AppHeaderItemCode.FINDING_AID,
        AppHeaderItemCode.ENTITY,
        AppHeaderItemCode.NEWS,
        AppHeaderItemCode.HELP,
    ],
}

const ConfigurationContext = createContext<ConfigurationType>(defaultConfiguration)

export const useConfiguration = () => useContext(ConfigurationContext)

export const ConfigurationProvider:FC = ({
    children
}) => {
let configuration = {...defaultConfiguration}
    try{
        configuration = {
            ...configuration,
            //@ts-ignore load configuration from global variable
            ...(_configuration ? _configuration : {})
        }
    } catch {
        configuration = {...defaultConfiguration}
    }
    return <ConfigurationContext.Provider value={configuration}>
        {children}
    </ConfigurationContext.Provider>
}

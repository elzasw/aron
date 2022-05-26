import React, { FC, useContext, createContext } from 'react';
import { AppHeaderItemCode, SearchOption } from '../../enums';

export interface ConfigurationType {
    allowDetailExpand?: boolean,
    alternativeItemLabel?: boolean;
    compactAppHeader?: boolean;
    showAppLogo?: boolean,
    showHeader?: boolean;
    showShareButtons?: boolean;
    showStandalonePartName?: boolean;
    showMainPageBreadcrumb?: boolean;
    headerItems?: AppHeaderItemCode[];
    searchOptions?: SearchOption[];
    localeCookieName?: string;
    showMetadataInImageViewer?: boolean;
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
    // localeCookieName: "pll_language",
    headerItems: [
        AppHeaderItemCode.FUND,
        AppHeaderItemCode.ARCH_DESC,
        AppHeaderItemCode.FINDING_AID,
        AppHeaderItemCode.ENTITY,
        AppHeaderItemCode.NEWS,
        AppHeaderItemCode.HELP,
    ],
    searchOptions: [
        SearchOption.ARCH_DESC_DAO_ONLY,
        SearchOption.ARCH_DESC,
        SearchOption.FUND,
        SearchOption.FINDING_AID,
        SearchOption.ENTITY,
    ]
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

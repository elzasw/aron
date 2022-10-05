import React, { FC, useContext, createContext } from 'react';
import { AppHeaderItemCode, SearchOption } from '../../enums';

interface DaoFooterDef {
  copyrightText?: string;
  links?: DaoFooterLinkDef[];
}

interface DaoFooterLinkDef {
  text?: string;
  url?: string;
}

export interface ConfigurationType {
    allowDetailExpand?: boolean,
    alternativeItemLabel?: boolean;
    compactAppHeader?: boolean;
    showAppLogo?: boolean,
    showAppTopImage?: boolean,
    showHeader?: boolean;
    showShareButtons?: boolean;
    showStandalonePartName?: boolean;
    showMainPageBreadcrumb?: boolean;
    headerItems?: AppHeaderItemCode[];
    searchOptions?: SearchOption[];
    localeCookieName?: string;
    showMetadataInImageViewer?: boolean;
    disableDownloads?: boolean;
    daoFooter?: DaoFooterDef;
}


const defaultConfiguration:ConfigurationType = {
    allowDetailExpand: true,
    alternativeItemLabel: false,
    compactAppHeader: false,
    showAppLogo: true,
    showAppTopImage: false,
    showHeader: true,
    showShareButtons: false,
    showStandalonePartName: true,
    showMainPageBreadcrumb: true,
    disableDownloads: false,
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
    ],
    // daoFooter: {
    //   copyrightText: 'Držitel licence: Literární archiv Památníku národního písemnictví, ' + new Date().getFullYear(),
    //   links: [{
    //     text: 'Smluvní podmínky',
    //     url: 'https://www.google.com',
    //   }]
    // },
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

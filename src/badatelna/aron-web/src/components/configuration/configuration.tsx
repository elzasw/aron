import React, { FC, useContext, createContext } from 'react';

export interface ConfigurationType {
    alternativeItemLabel?: boolean;
}

const defaultConfiguration = {
    alternativeItemLabel: false,
}

const ConfigurationContext = createContext<ConfigurationType>(defaultConfiguration)

export const useConfiguration = () => useContext(ConfigurationContext)

export const ConfigurationProvider:FC = ({
    children
}) => {
    const configuration = {
        ...defaultConfiguration,
        //@ts-ignore load configuration from global variable
        ..._configuration
    }
    return <ConfigurationContext.Provider value={configuration}>
        {children}
    </ConfigurationContext.Provider>
}

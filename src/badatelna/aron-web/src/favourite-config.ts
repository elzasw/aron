export const favouriteConfig = {
  // povinné (string nebo svg?)
  icon: 'SVG ikona',
  // povinné
  label: 'Název oblíbeného dotazu',
  // nepovinné
  query: 'Text fulltextového vyhledávání',
  // nepovinné
  type: 'ARCH_DESC | ENTITY | FINDING_AID | FUND',
  // nepovinné
  filters: [
    {
      // povinné při definici nového filtru
      type: 'CHECKBOX | CHECKBOX_WITH_RANGE | RADIO | INPUT | SELECT',
      // povinné při definici nového filtru
      label: 'Název filtru',
      // povinné
      field: 'Název pole pro filtrování',
      // povinné
      value: ['Hodnoty filtru'],
      // povinné při definici nového filtru
      options: [
        {
          // povinné
          value: 'Hodnota možnosti',
          // povinné
          label: 'Název možnosti',
        },
      ],
    },
  ],
};

export interface PageTemplate {
  homepage?: { 
    footerCenter?: string; 
    footerRight?: string 
  } | null;
  insitution: { name: string; link: string };
  localizations?: string[];
  name?: string;
}

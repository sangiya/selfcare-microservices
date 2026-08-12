# content-service -- porting TODO

`ContentArticle` is a best-guess consolidated shape for the Info/Help controller family below --
confirm it against the real data (especially `VasInfoController.php`, the largest controller in
this domain) before treating it as final; it may need to split into more than one document type
(e.g. articles vs. FAQ topics vs. video links).

## Controllers to port, ranked by size (Doc 1 sec 2.3 audit)

| Module | Controller | LOC | Notes |
|---|---|---:|---|
| scapp | VasInfoController | 2,692 | Largest in this domain -- read first. |
| scapp | InfoController | 1,019 | |
| scapp | InfoCategoriesController | 610 | |
| scapp | HelpCategoriesController | 444 | |
| webapp | ScreensController | 389 | Possibly overlaps config-tenant-service's layout model -- confirm before porting. |
| scapp | InfoSubCategoriesController | 364 | |
| scapp | HelpSubCategoriesController | 364 | |
| scapp | AppThemesController | 305 | Likely belongs in config-tenant-service's `TenantConfig.ThemeConfig`, not here -- confirm. |
| api | LanguageController | 280 | |
| webapp | HowToVideoController | 238 | Suggests a distinct "video" content type, not just text articles. |
| scapp | TermsConditionController | 228 | |
| api | HelperVideoController | 185 | |
| scapp | LanguageManagerController | 158 | |
| scapp | TopicController | 134 | |
| scapp | HelpController | 131 | |
| scapp | SearchController | 50 | Full-text search across articles -- consider Mongo Atlas Search or Elasticsearch (existing ELK) rather than building this from scratch. |

**16 controllers, ~7,600 LOC total** (Doc 1 sec 4.3) -- Low risk tier, Doc 5 90-day pilot scope.

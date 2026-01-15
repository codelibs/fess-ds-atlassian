JIRA/Confluence Data Store for Fess
[![Java CI with Maven](https://github.com/codelibs/fess-ds-atlassian/actions/workflows/maven.yml/badge.svg)](https://github.com/codelibs/fess-ds-atlassian/actions/workflows/maven.yml)
==========================

## Overview

JIRA/Confluence Data Store is an extension for Fess Data Store Crawling.

## Download

See [Maven Repository](https://repo1.maven.org/maven2/org/codelibs/fess/fess-ds-atlassian/).

## Installation

See [Plugin](https://fess.codelibs.org/13.3/admin/plugin-guide.html) of Administration guide.

## Getting Started

### Parameters

```
home=...
is_cloud=true
auth_type=...
oauth2.client_id=...
oauth2.client_secret=...
oauth2.access_token=...
oauth2.refresh_token=...
```

| Key | Value |
| --- | --- |
| home | URL of the Atlassian application(JIRA/Confluence) |
| is\_cloud | Whether the Atlassian instance is cloud-based (`true` or `false`)|
| auth_type | `oauth`(OAuth 1.0a authentication), `oauth2`(OAuth 2.0 authentication), or `basic`(Basic authentication) |
| oauth.consumer\_key | Consumer key for OAuth 1.0a (Usually `OauthKey`) |
| oauth.private\_key | Private key for OAuth 1.0a |
| oauth.secret | Verification code for OAuth 1.0a |
| oauth.access\_token | Access token for OAuth 1.0a |
| oauth2.client\_id | Client ID for OAuth 2.0 |
| oauth2.client\_secret | Client secret for OAuth 2.0 |
| oauth2.access\_token | Access token for OAuth 2.0 |
| oauth2.refresh\_token | Refresh token for OAuth 2.0 (Optional) |
| oauth2.token\_url | Token URL for OAuth 2.0 (Optional, has default) |
| basic.username | Username of Atlassian account for Basic authentication |
| basic.password | Password of Atlassian account for Basic authentication |
| issue.jql | [JQL](https://confluence.atlassian.com/jirasoftwarecloud/advanced-searching-764478330.html) for advanced search (JIRA only) (Optional) |

`oauth`, `oauth2`, or `basic` parameters are required.

### Scripts

#### JiraDataStore

```
url=issue.view_url
title=issue.summary
content=issue.description + issue.comments
last_modified=issue.last_modified
```

| Key | Value |
| --- | --- |
| issue.view\_url | URL of the issue. |
| issue.summary | Summary of the issue. |
| issue.description | Description of the issue. |
| issue.comments | Comments of the issue. |
| issue.last\_modified | Last modified date of the issue. |

#### ConfluenceDataStore

```
url=content.view_url
title=content.title
content=content.body + content.comments
last_modified=content.last_modified
```

| Key | Value |
| --- | --- |
| content.view\_url | URL of the content page. |
| content.title | Title of the content page. |
| content.body | Body of the content page. |
| content.comments | Comments of the content page. |
| content.last\_modified | Last modified date of the content. |

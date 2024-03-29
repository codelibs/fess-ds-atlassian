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
auth_type=...
oauth.consumer_key=OauthKey
oauth.private_key=...
oauth.secret=...
oauth.access_token=...
issue.jql=...
```

| Key | Value |
| --- | --- |
| home | URL of the Atlassian application(JIRA/Confluence) |
| auth_type | `oauth`(OAuth authentication) or `basic`(Basic authentication) |
| oauth.consumer\_key | Consumer key for OAuth (Usually `OauthKey`) |
| oauth.private\_key | Private key for OAuth |
| oauth.secret | Verification code for OAuth |
| oauth.access\_token | Access token for OAuth |
| basic.username | username of Atlassian account for Basic |
| basic.password | password of Atlassian account for Basic |
| issue.jql | [JQL](https://confluence.atlassian.com/jirasoftwarecloud/advanced-searching-764478330.html) for advanced search (JIRA only) (Optional) |

`oauth` or `basic` parameters are required.

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

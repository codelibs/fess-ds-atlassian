JIRA/Confluence Data Store for Fess
==========================

## Overview

JIRA/Confluence Data Store is an extension for Fess Data Store Crawling.

## Download

See [Maven Repository](http://central.maven.org/maven2/org/codelibs/fess/fess-ds-atlassian/).

## Installation

TBD

## Getting Started

### Parameters

```
home=https://example.atlassian.net
oauth.consumer_key=OauthKey
oauth.private_key=MIICdw...QE=
oauth.secret=qTJkPi
oauth.access_token=W1jjOV...ye
issue.jql=project = hoge
```

| Key | Value |
|-|-|
| home | URL of Atlassian applications(JIRA/Confluence) |
| oauth.consumer_key | consumer key for OAuth |
| oauth.private_key | private key for OAuth |
| oauth.secret | verification code for OAuth |
| oauth.access_token | access token for OAuth |
| basicauth.username | username of Atlassian account |
| basicauth.password | password of Atlassian account |
| issue.jql | [JQL](https://confluence.atlassian.com/jirasoftwarecloud/advanced-searching-764478330.html) for advanced search (JIRA only) (Optional) |

`oauth` or `basicauth` parameters are required.

### Scripts

#### JiraDataStore

```
url=issue.view_url
title=issue.summary
content=issue.description + issue.comments
last_modified=issue.last_modified
```

| Key | Value |
|-|-|
| issue.view_url | URL for viewing the issue |
| issue.summary | summary of the issue |
| issue.description | description of the issue |
| issue.comments | comments of the issue |
| issue.last_modified | last modified of the issue |

#### ConfluenceDataStore

```
url=contentview_url
title=content.title
content=content.body + content.comments
last_modified=content.last_modified
```

| Key | Value |
|-|-|
| content.view_url | URL for viewing the content page |
| content.title | title of the content page |
| content.body | body of the content page |
| content.comments | comments of the content page |
| content.last_modified | last modified of the content |
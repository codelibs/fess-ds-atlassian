JIRA/Confluence Data Store for Fess [![Build Status](https://travis-ci.org/codelibs/fess-ds-atlassian.svg?branch=master)](https://travis-ci.org/codelibs/fess-ds-atlassian)
==========================

## Overview

JIRA/Confluence Data Store is an extension for Fess Data Store Crawling.

## Download

See [Maven Repository](http://central.maven.org/maven2/org/codelibs/fess/fess-ds-atlassian/).

## Installation

### For 13.2.x and older

1. Download fess-ds-atlassian-X.X.X.jar
2. Copy fess-ds-atlassian-X.X.X.jar to $FESS\_HOME/app/WEB-INF/lib or /usr/share/fess/app/WEB-INF/lib

### For 13.3 and later

You can also install the plugin on the administration (See [the Administration guide](https://fess.codelibs.org/13.3/admin/plugin-guide.html)).

1. Download fess-ds-atlassian-X.X.X.jar
2. Copy fess-ds-atlassian-X.X.X.jar to $FESS\_HOME/app/WEB-INF/plugin or /usr/share/fess/app/WEB-INF/plugin

## Getting Started

### Parameters

```
home=https://example.atlassian.net
auth_type=oauth
oauth.consumer_key=OauthKey
oauth.private_key=MIICdw...QE=
oauth.secret=qTJkPi
oauth.access_token=W1jjOV...ye
issue.jql=project = hoge
```

| Key | Value |
| --- | --- |
| home | URL of Atlassian applications(JIRA/Confluence) |
| auth_type | `oauth` or `basic` |
| oauth.consumer\_key | consumer key for OAuth |
| oauth.private\_key | private key for OAuth |
| oauth.secret | verification code for OAuth |
| oauth.access\_token | access token for OAuth |
| basic.username | username of Atlassian account |
| basic.password | password of Atlassian account |
| issue.jql | [JQL](https://confluence.atlassian.com/jirasoftwarecloud/advanced-searching-764478330.html) for advanced search (JIRA only) (Optional) |

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
| issue.view\_url | URL for viewing the issue |
| issue.summary | summary of the issue |
| issue.description | description of the issue |
| issue.comments | comments of the issue |
| issue.last\_modified | last modified of the issue |

#### ConfluenceDataStore

```
url=content.view_url
title=content.title
content=content.body + content.comments
last_modified=content.last_modified
```

| Key | Value |
| --- | --- |
| content.view\_url | URL for viewing the content page |
| content.title | title of the content page |
| content.body | body of the content page |
| content.comments | comments of the content page |
| content.last\_modified | last modified of the content |

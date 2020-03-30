package ch.ivyteam.bitbucket.model.tag;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import ch.ivyteam.bitbucket.model.paged.PagedResult;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Tags extends PagedResult<Tag>
{
}

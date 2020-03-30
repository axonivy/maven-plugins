package ch.ivyteam.bitbucket.model.branch;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import ch.ivyteam.bitbucket.model.paged.PagedResult;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Branches extends PagedResult<Branch>
{
}

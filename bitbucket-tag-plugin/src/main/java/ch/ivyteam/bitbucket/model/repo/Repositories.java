package ch.ivyteam.bitbucket.model.repo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import ch.ivyteam.bitbucket.model.paged.PagedResult;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Repositories extends PagedResult<Repository>
{
}

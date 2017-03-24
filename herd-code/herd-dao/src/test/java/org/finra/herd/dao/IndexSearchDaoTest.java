/*
* Copyright 2015 herd contributors
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package org.finra.herd.dao;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Sets;
import org.apache.commons.collections4.CollectionUtils;
import org.elasticsearch.action.ListenableActionFuture;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.SearchShardTarget;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.internal.listeners.CollectCreatedMocks;
import org.mockito.internal.progress.MockingProgress;
import org.mockito.internal.progress.ThreadSafeMockingProgress;

import org.finra.herd.core.helper.ConfigurationHelper;
import org.finra.herd.dao.helper.ElasticsearchHelper;
import org.finra.herd.dao.impl.IndexSearchDaoImpl;
import org.finra.herd.model.api.xml.IndexSearchFilter;
import org.finra.herd.model.api.xml.IndexSearchKey;
import org.finra.herd.model.api.xml.IndexSearchRequest;
import org.finra.herd.model.api.xml.IndexSearchResponse;
import org.finra.herd.model.api.xml.IndexSearchResult;
import org.finra.herd.model.api.xml.IndexSearchResultTypeKey;
import org.finra.herd.model.api.xml.TagKey;
import org.finra.herd.model.dto.ConfigurationValue;
import org.finra.herd.model.dto.ElasticsearchResponseDto;
import org.finra.herd.model.dto.ResultTypeIndexSearchResponseDto;
import org.finra.herd.model.dto.TagIndexSearchResponseDto;
import org.finra.herd.model.dto.TagTypeIndexSearchResponseDto;

/**
 * IndexSearchDaoTest
 */
public class IndexSearchDaoTest extends AbstractDaoTest
{
    private static final String NAMESPACE = "namespace";

    private static final String TAG_TYPE = "tagType";

    private List<Object> createdMocks;

    @InjectMocks
    private IndexSearchDaoImpl indexSearchDao;

    @Mock
    private ConfigurationHelper configurationHelper;

    @Mock
    private TransportClientFactory transportClientFactory;

    @Mock
    private ElasticsearchHelper elasticsearchHelper;

    @Before
    public void before()
    {
        MockitoAnnotations.initMocks(this);
        createdMocks = new LinkedList<>();
        final MockingProgress progress = new ThreadSafeMockingProgress();
        progress.setListener(new CollectCreatedMocks(createdMocks));
    }

    @Test
    public void indexSearchTest()
    {
        // Create a new fields set that will be used when testing the index search method
        final Set<String> fields = Sets.newHashSet(DISPLAY_NAME_FIELD, SHORT_DESCRIPTION_FIELD);
        testIndexSearch(fields, null, null);
    }

    @Test
    public void indexSearchTestWithNoFields()
    {
        // Create a new fields set that will be used when testing the index search method
        final Set<String> fields = new HashSet<>();
        testIndexSearch(fields, null, null);
    }

    @Test
    public void indexSearchTestWithFacets()
    {
        // Create a new fields set that will be used when testing the index search method
        final Set<String> fields = new HashSet<>();
        //tag and result set facet
        testIndexSearch(fields, null, Arrays.asList(ElasticsearchHelper.RESULT_TYPE_FACET, ElasticsearchHelper.TAG_FACET));
    }

    @Test
    public void indexSearchTestWithTagFacet()
    {
        // Create a new fields set that will be used when testing the index search method
        final Set<String> fields = new HashSet<>();

        //tag facet only
        testIndexSearch(fields, null, Collections.singletonList(ElasticsearchHelper.TAG_FACET));
    }

    @Test
    public void indexSearchTestWithResultTypeFacet()
    {
        // Create a new fields set that will be used when testing the index search method
        final Set<String> fields = new HashSet<>();

        //result type facet only
        testIndexSearch(fields, null, Collections.singletonList(ElasticsearchHelper.RESULT_TYPE_FACET));
    }

    @Test
    public void indexSearchTestWithEmptyFilters()
    {
        // Create a new fields set that will be used when testing the index search method
        final Set<String> fields = new HashSet<>();

        // Create a new filters list
        List<IndexSearchFilter> searchFilters = new ArrayList<>();

        //result type facet only
        testIndexSearch(fields, searchFilters, null);
    }

    @Test
    public void indexSearchTestWithTagKeyFilter()
    {
        // Create a new fields set that will be used when testing the index search method
        final Set<String> fields = new HashSet<>();

        // Create an index search key
        final IndexSearchKey indexSearchKey = new IndexSearchKey();

        // Create a tag key
        final TagKey tagKey = new TagKey(TAG_TYPE_CODE, TAG_CODE);
        indexSearchKey.setTagKey(tagKey);

        // Create an index search keys list and add the previously defined key to it
        final List<IndexSearchKey> indexSearchKeys = Collections.singletonList(indexSearchKey);

        // Create an index search filter with the keys previously defined
        final IndexSearchFilter indexSearchFilter = new IndexSearchFilter(indexSearchKeys);

        List<IndexSearchFilter> indexSearchFilters = Collections.singletonList(indexSearchFilter);

        //result type facet only
        testIndexSearch(fields, indexSearchFilters, null);
    }

    @Test
    public void indexSearchTestWithResultTypeFilter()
    {
        // Create a new fields set that will be used when testing the index search method
        final Set<String> fields = new HashSet<>();

        // Create an index search key
        final IndexSearchKey indexSearchKey = new IndexSearchKey();

        // Create a result type key
        final IndexSearchResultTypeKey resultTypeKey = new IndexSearchResultTypeKey(BUSINESS_OBJECT_DEFINITION_INDEX);
        indexSearchKey.setIndexSearchResultTypeKey(resultTypeKey);

        // Create an index search keys list and add the previously defined key to it
        final List<IndexSearchKey> indexSearchKeys = Collections.singletonList(indexSearchKey);

        // Create an index search filter with the keys previously defined
        final IndexSearchFilter indexSearchFilter = new IndexSearchFilter(indexSearchKeys);

        List<IndexSearchFilter> indexSearchFilters = Collections.singletonList(indexSearchFilter);

        //result type facet only
        testIndexSearch(fields, indexSearchFilters, null);
    }

    private void testIndexSearch(Set<String> fields, List<IndexSearchFilter> searchFilters, List<String> facetList)
    {
        // Build the mocks
        SearchRequestBuilder searchRequestBuilder = mock(SearchRequestBuilder.class);
        SearchRequestBuilder searchRequestBuilderWithSource = mock(SearchRequestBuilder.class);
        SearchRequestBuilder searchRequestBuilderWithSize = mock(SearchRequestBuilder.class);
        SearchRequestBuilder searchRequestBuilderWithBusinessObjectDefinitionIndexBoost = mock(SearchRequestBuilder.class);
        SearchRequestBuilder searchRequestBuilderWithTagIndexBoost = mock(SearchRequestBuilder.class);
        SearchRequestBuilder searchRequestBuilderWithSorting = mock(SearchRequestBuilder.class);

        TransportClient transportClient = mock(TransportClient.class);

        SearchResponse searchResponse = mock(SearchResponse.class);
        SearchHits searchHits = mock(SearchHits.class);
        SearchHit searchHit1 = mock(SearchHit.class);
        SearchHit searchHit2 = mock(SearchHit.class);
        SearchShardTarget searchShardTarget1 = mock(SearchShardTarget.class);
        SearchShardTarget searchShardTarget2 = mock(SearchShardTarget.class);
        SearchHit[] searchHitArray = new SearchHit[2];
        searchHitArray[0] = searchHit1;
        searchHitArray[1] = searchHit2;

        @SuppressWarnings("unchecked")
        ListenableActionFuture<SearchResponse> listenableActionFuture = mock(ListenableActionFuture.class);

        // Mock the call to external methods
        when(configurationHelper.getProperty(ConfigurationValue.TAG_SHORT_DESCRIPTION_LENGTH, Integer.class)).thenReturn(300);
        when(configurationHelper.getProperty(ConfigurationValue.BUSINESS_OBJECT_DEFINITION_SHORT_DESCRIPTION_LENGTH, Integer.class)).thenReturn(300);
        when(transportClientFactory.getTransportClient()).thenReturn(transportClient);
        when(transportClient.prepareSearch(BUSINESS_OBJECT_DEFINITION_INDEX, TAG_INDEX)).thenReturn(searchRequestBuilder);
        when(searchRequestBuilder.setSource(any())).thenReturn(searchRequestBuilderWithSource);
        when(searchRequestBuilderWithSource.setSize(SEARCH_RESULT_SIZE)).thenReturn(searchRequestBuilderWithSize);
        when(searchRequestBuilderWithSize.addIndexBoost(BUSINESS_OBJECT_DEFINITION_INDEX, BUSINESS_OBJECT_DEFINITION_INDEX_BOOST))
            .thenReturn(searchRequestBuilderWithBusinessObjectDefinitionIndexBoost);
        when(searchRequestBuilderWithBusinessObjectDefinitionIndexBoost.addIndexBoost(TAG_INDEX, TAG_INDEX_BOOST))
            .thenReturn(searchRequestBuilderWithTagIndexBoost);
        when(searchRequestBuilderWithTagIndexBoost.addSort(any())).thenReturn(searchRequestBuilderWithSorting);
        when(searchRequestBuilder.execute()).thenReturn(listenableActionFuture);
        when(listenableActionFuture.actionGet()).thenReturn(searchResponse);
        when(searchResponse.getHits()).thenReturn(searchHits);
        when(searchHits.hits()).thenReturn(searchHitArray);
        Map<String, Object> sourceMap1 = new HashMap<>();
        Map<String, Object> tagTypeMap = new HashMap<>();
        tagTypeMap.put(CODE, TAG_TYPE_CODE);
        sourceMap1.put(TAG_TYPE, tagTypeMap);
        when(searchHit1.sourceAsMap()).thenReturn(sourceMap1);
        Map<String, Object> sourceMap2 = new HashMap<>();
        Map<String, Object> businessObjectDefinitionMap = new HashMap<>();
        businessObjectDefinitionMap.put(CODE, NAMESPACE_CODE);
        sourceMap2.put(NAMESPACE, businessObjectDefinitionMap);
        when(searchHit2.sourceAsMap()).thenReturn(sourceMap2);
        when(searchHit1.getShard()).thenReturn(searchShardTarget1);
        when(searchHit2.getShard()).thenReturn(searchShardTarget2);
        when(searchShardTarget1.getIndex()).thenReturn(TAG_INDEX);
        when(searchShardTarget2.getIndex()).thenReturn(BUSINESS_OBJECT_DEFINITION_INDEX);
        when(searchHits.getTotalHits()).thenReturn(200L);

        // Create index search request
        final IndexSearchRequest indexSearchRequest = new IndexSearchRequest(SEARCH_TERM, searchFilters, facetList);

        List<TagTypeIndexSearchResponseDto> tagTypeIndexSearchResponseDtos =
            Collections.singletonList(new TagTypeIndexSearchResponseDto("code", 1, Collections.singletonList(new TagIndexSearchResponseDto("tag1", 1))));
        List<ResultTypeIndexSearchResponseDto> resultTypeIndexSearchResponseDto =
            Collections.singletonList(new ResultTypeIndexSearchResponseDto("type", 1, null));

        when(elasticsearchHelper.getNestedTagTagIndexSearchResponseDto(searchResponse)).thenReturn(tagTypeIndexSearchResponseDtos);
        when(elasticsearchHelper.getResultTypeIndexSearchResponseDto(searchResponse)).thenReturn(resultTypeIndexSearchResponseDto);
        when(elasticsearchHelper.getFacetsResponse(any(ElasticsearchResponseDto.class), any(Boolean.class))).thenCallRealMethod();
        when(elasticsearchHelper.addIndexSearchFilterBooleanClause(any(List.class))).thenCallRealMethod();
        when(elasticsearchHelper.addFacetFieldAggregations(any(Set.class), any(SearchRequestBuilder.class))).thenCallRealMethod();

        // Call the method under test
        IndexSearchResponse indexSearchResponse = indexSearchDao.indexSearch(indexSearchRequest, fields);
        List<IndexSearchResult> indexSearchResults = indexSearchResponse.getIndexSearchResults();

        assertThat("Index search results list is null.", indexSearchResults, not(nullValue()));
        assertThat(indexSearchResults.size(), is(2));
        assertThat(indexSearchResponse.getTotalIndexSearchResults(), is(200L));

        int facetSize = 0;

        if (facetList != null && facetList.contains(ElasticsearchHelper.RESULT_TYPE_FACET))
        {
            facetSize++;
        }
        if (facetList != null && facetList.contains(ElasticsearchHelper.TAG_FACET))
        {
            facetSize++;
        }
        assertThat(indexSearchResponse.getFacets() != null ? indexSearchResponse.getFacets().size() : 0, is(facetSize));

        // Verify the calls to external methods
        verify(configurationHelper, times(1)).getProperty(ConfigurationValue.TAG_SHORT_DESCRIPTION_LENGTH, Integer.class);
        verify(configurationHelper, times(1)).getProperty(ConfigurationValue.BUSINESS_OBJECT_DEFINITION_SHORT_DESCRIPTION_LENGTH, Integer.class);
        verify(transportClient, times(1)).prepareSearch(BUSINESS_OBJECT_DEFINITION_INDEX, TAG_INDEX);
        verify(searchRequestBuilder, times(1)).setSource(any());

        if (CollectionUtils.isNotEmpty(facetList))
        {
            verifyFacetInvocations(searchRequestBuilder, facetList);
        }

        verify(searchRequestBuilderWithSource, times(1)).setSize(SEARCH_RESULT_SIZE);
        verify(searchRequestBuilderWithSize, times(1)).addIndexBoost(BUSINESS_OBJECT_DEFINITION_INDEX, BUSINESS_OBJECT_DEFINITION_INDEX_BOOST);
        verify(searchRequestBuilderWithBusinessObjectDefinitionIndexBoost, times(1)).addIndexBoost(TAG_INDEX, TAG_INDEX_BOOST);
        verify(searchRequestBuilderWithTagIndexBoost, times(1)).addSort(any());
        verify(searchRequestBuilder, times(1)).execute();
        verify(listenableActionFuture, times(1)).actionGet();
        verify(searchResponse, times(1)).getHits();
        verify(searchHits, times(1)).hits();
        verify(searchHit1, times(1)).sourceAsMap();
        verify(searchHit2, times(1)).sourceAsMap();
        verify(searchHit1, times(1)).getShard();
        verify(searchHit2, times(1)).getShard();
        verify(searchShardTarget1, times(1)).getIndex();
        verify(searchShardTarget2, times(1)).getIndex();
        verify(searchHits, times(1)).getTotalHits();
        verifyNoMoreInteractions(createdMocks.toArray());
    }

    private void verifyFacetInvocations(SearchRequestBuilder searchRequestBuilder, List<String> facetList)
    {
        if (facetList.contains(ElasticsearchHelper.TAG_FACET) && !facetList.contains(ElasticsearchHelper.RESULT_TYPE_FACET))
        {
            verify(searchRequestBuilder, times(2)).addAggregation(any(AggregationBuilder.class));
        }
        else if (!facetList.contains(ElasticsearchHelper.TAG_FACET) && facetList.contains(ElasticsearchHelper.RESULT_TYPE_FACET))
        {
            verify(searchRequestBuilder, times(1)).addAggregation(any(AggregationBuilder.class));
        }
        else if (facetList.contains(ElasticsearchHelper.TAG_FACET) && facetList.contains(ElasticsearchHelper.RESULT_TYPE_FACET))
        {
            verify(searchRequestBuilder, times(3)).addAggregation(any(AggregationBuilder.class));
        }
    }
}
package com.tbar.MakeupStoreApplication.service;

import com.tbar.MakeupStoreApplication.service.consumer.MultiAPIConsumer;
import com.tbar.MakeupStoreApplication.service.consumer.SoloAPIConsumer;
import com.tbar.MakeupStoreApplication.service.consumer.model.Item;
import com.tbar.MakeupStoreApplication.utility.AppProperties;
import com.tbar.MakeupStoreApplication.utility.exceptions.consumerLayer.APICallClientSideException;
import com.tbar.MakeupStoreApplication.utility.exceptions.consumerLayer.APICallNotFoundException;
import com.tbar.MakeupStoreApplication.utility.exceptions.consumerLayer.APICallServerSideException;
import com.tbar.MakeupStoreApplication.utility.exceptions.serviceLayer.APIConnectionException;
import com.tbar.MakeupStoreApplication.utility.exceptions.serviceLayer.ProductNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.URI;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MakeupServiceImplTest {

    // === constants ===
    private final URI STUB_BASE_URI = URI.create("http://www.example.com");
    private final Set<String> STUB_VALID_PARAMETERS = new HashSet<>(Set.of("first", "second"));
    private final String STUB_URI_SUFFIX = ".json";

    private final Item EXPECTED_ITEM = new Item();
    private final List<Item> EXPECTED_LIST = new ArrayList<>(List.of(EXPECTED_ITEM));

    private final Long EXAMPLE_ID = 1000L;
    private final String FIRST_ENTRY_KEY = "first";
    private final String FIRST_ENTRY_VALUE = "value";
    private final String SECOND_ENTRY_KEY = "second";
    private final String SECOND_ENTRY_VALUE = "value2";
    private final URI URI_WITH_ID_PATH = URI.create(STUB_BASE_URI + "/" + EXAMPLE_ID + STUB_URI_SUFFIX);
    private final URI URI_WITH_TWO_PARAMETERS = URI.create(String.format("%s?%s=%s&%s=%s", STUB_BASE_URI, FIRST_ENTRY_KEY, FIRST_ENTRY_VALUE, SECOND_ENTRY_KEY, SECOND_ENTRY_VALUE));
    private final Map<String, String> MAP_WITH_VALID_PARAMETERS = new LinkedHashMap<>();
    private final Map<String, String> MAP_WITH_MIXED_PARAMETERS = new LinkedHashMap<>();
    private final Map<String, String> MAP_WITH_WRONG_PARAMETERS = new LinkedHashMap<>();
    // === fields ===
    @Mock
    private SoloAPIConsumer soloSearchConsumerMock;
    @Mock
    private MultiAPIConsumer multiSearchConsumerMock;
    private MakeupService makeupService;
    private AppProperties appProperties = new AppProperties();

    // === constructors ===
    MakeupServiceImplTest() {
        // initialize fields that are injected from properties file
        ReflectionTestUtils.setField(appProperties, "makeupApiMultiBaseUri", STUB_BASE_URI.toString());
        ReflectionTestUtils.setField(appProperties, "makeupApiSoloBaseUri", STUB_BASE_URI.toString());
        ReflectionTestUtils.setField(appProperties, "makeupApiSoloUriSuffix", STUB_URI_SUFFIX);
        ReflectionTestUtils.setField(appProperties, "makeupApiValidParameters", STUB_VALID_PARAMETERS.toArray(new String[0]));
        
        // Putting here just to ensure proper order of entries. If wasn't tests may occasionally crush.
        MAP_WITH_VALID_PARAMETERS.put(FIRST_ENTRY_KEY, FIRST_ENTRY_VALUE);
        MAP_WITH_VALID_PARAMETERS.put(SECOND_ENTRY_KEY, SECOND_ENTRY_VALUE);

        MAP_WITH_MIXED_PARAMETERS.put(FIRST_ENTRY_KEY, FIRST_ENTRY_VALUE);
        MAP_WITH_MIXED_PARAMETERS.put(SECOND_ENTRY_KEY, SECOND_ENTRY_VALUE);
        MAP_WITH_MIXED_PARAMETERS.put("third", "value3");

        MAP_WITH_WRONG_PARAMETERS.put("third", "value3");
        MAP_WITH_WRONG_PARAMETERS.put("fourth", "value4");
    }

    // === initialization ===
    @BeforeEach
    void initialize() {
        makeupService = new MakeupServiceImpl(appProperties, multiSearchConsumerMock, soloSearchConsumerMock);
    }

    // === tests ===
        // === getProducts method ===
            // === proper behaviour ===
    @Test
    void given_mapOfValidParameters_when_getProducts_return_listOfItems() {
        when(multiSearchConsumerMock.requestData(URI_WITH_TWO_PARAMETERS)).thenReturn(new ResponseEntity<>(List.of(EXPECTED_ITEM), HttpStatus.OK));

        List<Item> actualList = makeupService.getProducts(MAP_WITH_VALID_PARAMETERS);

        assertEquals(EXPECTED_LIST, actualList);
    }

    @Test
    void given_mapOfMixedParameters_when_getProducts_return_listOfItems() {
        when(multiSearchConsumerMock.requestData(URI_WITH_TWO_PARAMETERS)).thenReturn(new ResponseEntity<>(List.of(EXPECTED_ITEM), HttpStatus.OK));

        List<Item> actualList = makeupService.getProducts(MAP_WITH_MIXED_PARAMETERS);

        assertEquals(EXPECTED_LIST, actualList);
    }

    @Test
    void given_mapOfWrongParameters_when_getProducts_return_listOfItems() {
        when(multiSearchConsumerMock.requestData(STUB_BASE_URI)).thenReturn(new ResponseEntity<>(List.of(EXPECTED_ITEM), HttpStatus.OK));

        List<Item> actualList = makeupService.getProducts(MAP_WITH_WRONG_PARAMETERS);

        assertEquals(EXPECTED_LIST, actualList);
    }

    @Test
    void given_nullMap_when_getProducts_return_listOfItems() {
        when(multiSearchConsumerMock.requestData(STUB_BASE_URI)).thenReturn(new ResponseEntity<>(List.of(EXPECTED_ITEM), HttpStatus.OK));

        List<Item> actualList = makeupService.getProducts(null);

        assertEquals(EXPECTED_LIST, actualList);
    }

    @Test
    void given_emptyMap_when_getProducts_return_listOfItems() {
        when(multiSearchConsumerMock.requestData(STUB_BASE_URI)).thenReturn(new ResponseEntity<>(List.of(EXPECTED_ITEM), HttpStatus.OK));

        List<Item> actualList = makeupService.getProducts(new HashMap<>());

        assertEquals(EXPECTED_LIST, actualList);
    }

            // === errors ===
    @Test
    void given_responseThrowAPICallNotFoundException_when_getProducts_throw_ProductNotFoundException() {
        when(multiSearchConsumerMock.requestData(URI_WITH_TWO_PARAMETERS)).thenThrow(new APICallNotFoundException());

        assertThrows(ProductNotFoundException.class, ()-> makeupService.getProducts(MAP_WITH_VALID_PARAMETERS));
    }

    @Test
    void given_responseThrowAPICallServerSideException_when_getProducts_throw_APIConnectionException() {
        when(multiSearchConsumerMock.requestData(URI_WITH_TWO_PARAMETERS)).thenThrow(new APICallServerSideException(503));

        assertThrows(APIConnectionException.class, ()-> makeupService.getProducts(MAP_WITH_VALID_PARAMETERS));
    }

    @Test
    void given_responseThrowAPICallClientSideException_when_getProducts_throw_APIConnectionException() {
        when(multiSearchConsumerMock.requestData(URI_WITH_TWO_PARAMETERS)).thenThrow(new APICallClientSideException(400));

        assertThrows(APIConnectionException.class, ()-> makeupService.getProducts(MAP_WITH_VALID_PARAMETERS));
    }

    @Test
    void given_responseBodyIsEmpty_when_getProducts_throw_ProductNotFoundException() {
        when(multiSearchConsumerMock.requestData(URI_WITH_TWO_PARAMETERS)).thenReturn(new ResponseEntity<>(new ArrayList<>(), HttpStatus.OK));

        assertThrows(ProductNotFoundException.class, ()-> makeupService.getProducts(MAP_WITH_VALID_PARAMETERS));
    }

    @Test
    void given_responseBodyIsNull_when_getProducts_throw_ProductNotFoundException() {
        when(multiSearchConsumerMock.requestData(URI_WITH_TWO_PARAMETERS)).thenReturn(new ResponseEntity<>(null, HttpStatus.OK));

        assertThrows(ProductNotFoundException.class, ()-> makeupService.getProducts(MAP_WITH_VALID_PARAMETERS));
    }

        // === getProduct method ===
    @Test
    void given_properId_when_getProduct_return_Item() {
        when(soloSearchConsumerMock.requestData(URI_WITH_ID_PATH)).thenReturn(new ResponseEntity<>(EXPECTED_ITEM, HttpStatus.OK));

        Item actualItem = makeupService.getProduct(EXAMPLE_ID);

        assertEquals(EXPECTED_ITEM, actualItem);
    }

    @Test
    void given_responseThrowAPICallNotFoundException_when_getProduct_throw_ProductNotFoundException() {
        when(soloSearchConsumerMock.requestData(URI_WITH_ID_PATH)).thenThrow(new APICallNotFoundException());

        assertThrows(ProductNotFoundException.class, ()-> makeupService.getProduct(EXAMPLE_ID));
    }

    @Test
    void given_responseThrowAPICallServerSideException_when_getProduct_throw_APIConnectionException() {
        when(soloSearchConsumerMock.requestData(URI_WITH_ID_PATH)).thenThrow(new APICallServerSideException(503));

        assertThrows(APIConnectionException.class, ()-> makeupService.getProduct(EXAMPLE_ID));
    }

    @Test
    void given_responseThrowAPICallClientSideException_when_getProduct_throw_APIConnectionException() {
        when(soloSearchConsumerMock.requestData(URI_WITH_ID_PATH)).thenThrow(new APICallClientSideException(400));

        assertThrows(APIConnectionException.class, ()-> makeupService.getProduct(EXAMPLE_ID));
    }

    @Test
    void given_responseWithOkStatusAndNullBody_when_getProduct_throw_ProductNotFoundException() {
        when(soloSearchConsumerMock.requestData(URI_WITH_ID_PATH)).thenReturn(new ResponseEntity<>(null, HttpStatus.OK));

        assertThrows(ProductNotFoundException.class, ()-> makeupService.getProduct(EXAMPLE_ID));
    }
}
# İkas Admin API — işlem envanteri

Kaynak: canlı introspection (2026-10-28), `sema/ikas-admin-sema.json`. Argümanlar ve dönüş tipleri; `!` = zorunlu.
Toplam **135 işlem**.

## Sorgular (65)

- **getAuthorizedApp** → `AuthorizedApp` — —  
  By using this api you can get properties of merchant and merchant staff.
- **getAvailableSubscriptions** → `[GetAvailableSubscriptionResponse!]!` — —  
  Using this api, you can get available subscriptions for a merchant app.
- **getGlobalTaxSettings** → `GlobalTaxSettings` — —  
  Use this mutation to get global settings.
- **getIkasWalletWithBalance** → `GetIkasWalletWithBalanceResponse!` — `input: GetIkasWalletWithBalanceInput!`  
  Using this API, you can make payments and refunds via Ikas Wallet.
- **getImageUploadUrl** → `String!` — `contentType: String`; `imageDir: String`; `imageId: String!`  
  Upload url for the specified image.
- **getImportJobData** → `GetImportJobDataResponse!` — `importJobId: String!`; `importType: ImportTypeEnum!`
- **getImportJobDataList** → `GetLastImportJobDataResponseData!` — —
- **getLastImportJobData** → `GetLastImportJobDataResponse!` — —
- **getMerchant** → `MerchantResponse!` — —  
  By using this api you can get properties of merchant and merchant staff.
- **getMerchantLicence** → `MerchantLicence!` — —  
  Using this api you can view all licenses owned by merchant.
- **getSalesChannel** → `SalesChannel` — —  
  Using this api, you can view your sales channel.
- **getTimelineEntry** → `String!` — —
- **getVideoUploadUrl** → `GetVideoUploadURLResponse!` — `videoId: String!`
- **listAbandonedCheckouts** → `CartPaginationResponse!` — `customerId: StringFilterInput`; `id: StringFilterInput`; `input: ListAbandonedCartInput!`; `lastActivityDate: DateFilterInput`; `mailSendDate: DateFilterInput`; `pagination: PaginationInput`; `sort: String`  
  Use this query to get abandoned checkouts.
- **listBranch** → `[Branch!]!` — `id: StringFilterInput`; `includeDeleted: Boolean`; `name: StringFilterInput`; `salesChannelId: StringFilterInput`; `updatedAt: DateFilterInput`
- **listCampaign** → `CampaignPaginationResponse!` — `canCombineWithOtherCampaigns: BooleanFilterInput`; `dateRangeIsActive: CampaignDateRangeIsActiveFilterInput`; `hasCoupon: BooleanFilterInput`; `id: StringFilterInput`; `includeDeleted: Boolean`; `pagination: PaginationInput`; `search: String`; `sort: String`; `type: CampaignTypeEnumFilterInput`; `updatedAt: DateFilterInput`  
  Query to list Campaign Search functionality is available for the following fields: `title`. Example: If search='test', it will match records containing 'test' in any of these fields.
- **listCampaignOffer** → `CampaignOfferPaginationResponse!` — `id: StringFilterInput`; `pagination: PaginationInput`; `search: String`; `sort: String`; `updatedAt: DateFilterInput`
- **listCargoCompany** → `[CargoCompany!]!` — `id: StringFilterInput`; `includeDeleted: Boolean`
- **listCategory** → `[Category!]!` — `categoryPath: CategoryPathFilterInput`; `id: StringFilterInput`; `search: String`; `updatedAt: DateFilterInput`  
  Using this api, you can view the categories of products. Search functionality is available for the following fields: `name`. Example: If search='test', it will match records containing 'test' in any of these fields.
- **listCity** → `[City!]!` — `countryId: StringFilterInput`; `id: StringFilterInput`; `includeDeleted: Boolean`; `search: String`; `stateId: StringFilterInput!`; `updatedAt: DateFilterInput`
- **listCountry** → `[Country!]!` — `id: StringFilterInput`; `includeDeleted: Boolean`; `iso2: StringFilterInput`; `iso3: StringFilterInput`; `locale: String`; `search: String`; `updatedAt: DateFilterInput`
- **listCoupon** → `CouponPaginationResponse!` — `campaignId: StringFilterInput`; `id: StringFilterInput`; `includeDeleted: Boolean`; `pagination: PaginationInput`; `search: String`
- **listCurrency** → `[Currency!]!` — `code: StringFilterInput`
- **listCustomer** → `CustomerPaginationResponse!` — `email: StringFilterInput`; `id: StringFilterInput`; `merchantId: StringFilterInput`; `pagination: PaginationInput`; `phone: StringFilterInput`; `search: String`; `sort: String`; `updatedAt: DateFilterInput`  
  Use this mutation to list customers as paginated by supplying the `CustomerArgs` input. Search functionality is available for the following fields: `firstName`, `lastName`, `email`, `phone`. Example: If search='test', it will match records containing 'test' in any of these fields. Sorting is available for the following fields: `updatedAt`. Sort syntax: - Ascending: `fieldName` (e.g., `createdAt`) - Descending: `-fieldName` (e.g., `-createdAt`)
- **listCustomerAttribute** → `[CustomerAttribute!]!` — `includeDeleted: Boolean`
- **listCustomerGroup** → `[CustomerGroup!]!` — `id: StringFilterInput`; `includeDeleted: Boolean`; `name: StringFilterInput`; `updatedAt: DateFilterInput`  
  Query to list CustomerGroup
- **listCustomerTag** → `[CustomerTag!]!` — `id: StringFilterInput`; `includeDeleted: Boolean`; `name: StringFilterInput`; `updatedAt: DateFilterInput`  
  Query to list CustomerTag
- **listDistrict** → `[District!]!` — `cityId: StringFilterInput!`; `countryId: StringFilterInput`; `id: StringFilterInput`; `includeDeleted: Boolean`; `search: String`; `stateId: StringFilterInput`; `updatedAt: DateFilterInput`
- **listGlobalTaxSettings** → `[GlobalTaxSettings!]!` — `id: StringFilterInput`; `includeDeleted: Boolean`; `storefrontId: StringFilterInput`  
  Query to list GlobalTaxSettings
- **listIkasWallet** → `[IkasWallet!]!` — `id: StringFilterInput`
- **listIkasWalletTransaction** → `IkasWalletTransactionPaginationResponse!` — `pagination: PaginationInput`; `status: IkasWalletTransactionStatusEnumFilter`; `transactionId: StringFilterInput`
- **listLanguage** → `[Language!]!` — `id: StringFilterInput`
- **listMerchantAppPayment** → `MerchantAppPaymentPaginationResponse!` — `id: StringFilterInput`; `pagination: PaginationInput`  
  Using this api, you can view the payment features that a merchant has created for the app.
- **listMerchantSettings** → `[MerchantSettings!]!` — `id: StringFilterInput`; `merchantId: StringFilterInput`
- **listOrder** → `OrderPaginationResponse!` — `branchId: StringFilterInput`; `branchSessionId: StringFilterInput`; `closedAt: DateFilterInput`; `customerEmail: StringFilterInput`; `customerId: StringFilterInput`; `id: StringFilterInput`; `invoicesStoreAppId: StringFilterInput`; `orderNumber: StringFilterInput`; `orderPackageStatus: OrderPackageStatusEnumInputFilter`; `orderPaymentStatus: OrderPaymentStatusEnumInputFilter`; `orderTagIds: StringFilterInput`; `orderedAt: DateFilterInput`; `pagination: PaginationInput`; `paymentMethodType: OrderPaymentMethodEnumFilterInput`; `salesChannelId: StringFilterInput`; `search: String`; `shippingMethod: OrderShippingMethodEnumFilterInput`; `sort: String`; `sourceId: StringFilterInput`; `status: OrderStatusEnumInputFilter`; `stockLocationId: StringFilterInput`; `terminalId: StringFilterInput`; `updatedAt: DateFilterInput`
- **listOrderSession** → `[OrderSession!]!` — `includeDeleted: Boolean`; `orderId: StringFilterInput!`
- **listOrderTag** → `[OrderTag!]!` — `id: StringFilterInput`; `includeDeleted: Boolean`; `name: StringFilterInput`; `search: String`; `updatedAt: DateFilterInput`  
  Query to list OrderTag Search functionality is available for the following fields: `name`. Example: If search='test', it will match records containing 'test' in any of these fields.
- **listOrderTransactions** → `[PublicTransaction!]!` — `includeAll: Boolean`; `orderId: String!`
- **listPaymentGateway** → `[PaymentGateway!]!` — `cartId: String`; `id: StringFilterInput`; `includeDeleted: Boolean`; `locale: String`; `pagination: PaginationInput`; `transactionAmount: Float`
- **listPriceList** → `[PriceList!]!` — `id: StringFilterInput`
- **listProduct** → `ProductPaginationResponse!` — `attributeId: ProductAttributeFilterInput`; `barcodeList: StringFilterInput`; `brandId: StringFilterInput`; `bundleProductsProductId: StringFilterInput`; `categoryIds: CategoryFilterInput`; `id: StringFilterInput`; `includeDeleted: Boolean`; `name: StringFilterInput`; `pagination: PaginationInput`; `salesChannelIds: SalesChannelFilterInput`; `search: String`; `sku: StringFilterInput`; `sort: String`; `tagIds: StringFilterInput`; `variantStockLocationId: StringFilterInput`; `variantTypeId: StringFilterInput`; `vendorId: StringFilterInput`  
  Use this query to list products. Search functionality is available for the following fields: `name`, `variants.sku`, `variants.barcodeList`. Example: If search='test', it will match records containing 'test' in any of these fields. Sorting is available for the following fields: `createdAt`, `updatedAt`, `name`. Sort syntax: - Ascending: `fieldName` (e.g., `createdAt`) - Descending: `-fieldName` (e.g., `-createdAt`)
- **listProductAttribute** → `[ProductAttribute!]!` — `id: StringFilterInput`; `name: StringFilterInput`; `updatedAt: DateFilterInput`  
  Use this query to list product attributes.
- **listProductBrand** → `[ProductBrand!]!` — `id: StringFilterInput`; `search: String`; `updatedAt: DateFilterInput`  
  Using this api, you can view the brands of products. Search functionality is available for the following fields: `name`. Example: If search='test', it will match records containing 'test' in any of these fields.
- **listProductOptionSet** → `[ProductOptionSet!]!` — `id: StringFilterInput`; `search: String`; `updatedAt: DateFilterInput`
- **listProductOrder** → `[ProductOrder!]!` — `brandIds: StringFilterInput`; `categoryIds: StringFilterInput`; `id: StringFilterInput`; `includeDeleted: Boolean`; `updatedAt: DateFilterInput`  
  Query to list ProductOrder
- **listProductStockLocation** → `ProductStockLocationPaginationResponse!` — `id: StringFilterInput`; `pagination: PaginationInput`; `productId: StringFilterInput`; `sort: String`; `stockLocationId: StringFilterInput`; `updatedAt: DateFilterInput`; `variantId: StringFilterInput`
- **listProductTag** → `[ProductTag!]!` — `id: StringFilterInput`; `name: StringFilterInput`; `search: String`; `updatedAt: DateFilterInput`  
  Using this api, you can view the tags of products. Search functionality is available for the following fields: `name`. Example: If search='test', it will match records containing 'test' in any of these fields.
- **listProductUnit** → `[ProductUnit!]!` — `id: StringFilterInput`  
  Using this api, you can view the units of products.
- **listProductVolumeDiscount** → `[ProductVolumeDiscount!]!` — `id: StringFilterInput`  
  Using this api, you can view the product volume discounts.
- **listSalesChannel** → `[SalesChannel!]!` — `id: StringFilterInput`
- **listShippingSettings** → `[ShippingSettings!]!` — `includeDeleted: Boolean`
- **listState** → `[State!]!` — `countryId: StringFilterInput!`; `id: StringFilterInput`; `includeDeleted: Boolean`; `locale: String`; `search: String`; `updatedAt: DateFilterInput`
- **listStockLocation** → `[StockLocation!]!` — `id: StringFilterInput`; `name: StringFilterInput`; `updatedAt: DateFilterInput`
- **listStoreCredit** → `[StoreCredit!]!` — `customerId: String!`  
  List store credit records for one customer. Each sales-channel / storefront row is returned separately; remaining balances are not summed. Deleted and gift-card records are omitted.
- **listStorefront** → `[Storefront!]!` — `id: StringFilterInput`; `salesChannelId: StringFilterInput`
- **listStorefrontJSScript** → `[StorefrontJSScript!]!` — `storefrontId: String`  
  Use this query to list storefront javascript scripts by supplying the `storefrontId` input.
- **listStorefrontPolicy** → `[StorefrontPolicy!]!` — `id: StringFilterInput`; `includeDeleted: Boolean`; `language: StringFilterInput`; `name: StringFilterInput`; `pagination: PaginationInput`; `search: String`; `updatedAt: DateFilterInput`  
  Use this query to list storefront policies by supplying the `StorefrontPolicyArgs` input. Search functionality is available for the following fields: `name`, `slug`. Example: If search='test', it will match records containing 'test' in any of these fields.
- **listTaxSettings** → `[TaxSettings!]!` — `countryId: StringFilterInput`; `id: StringFilterInput`; `includeDeleted: Boolean`  
  Query to list TaxSettings
- **listTerminal** → `[Terminal!]!` — `branchId: StringFilterInput`; `id: StringFilterInput`; `includeDeleted: Boolean`; `updatedAt: DateFilterInput`
- **listTown** → `[Town!]!` — `districtId: StringFilterInput!`; `id: StringFilterInput`; `includeDeleted: Boolean`; `search: String`; `updatedAt: DateFilterInput`
- **listVariantType** → `[VariantType!]!` — `id: StringFilterInput`; `name: StringFilterInput`; `updatedAt: DateFilterInput`  
  Using this api, you can view the variant types of products.
- **listVendor** → `[Vendor!]!` — `id: StringFilterInput`; `includeDeleted: Boolean`; `merchantId: StringFilterInput`; `name: StringFilterInput`; `updatedAt: DateFilterInput`  
  Use this query to list the vendor.
- **listWebhook** → `[Webhook!]!` — —  
  Use this query to list active webhooks of your application.
- **me** → `MeResponse` — —
- **searchProducts** → `ProductSearchResponse!` — `input: SearchInput!`  
  Result(s) containing searched products.

## Mutasyonlar (70)

- **addCustomTimelineEntry** → `Boolean!` — `input: TimelineInput!`
- **addOrderInvoice** → `Order` — `input: AddOrderInvoiceInput!`  
  Use this mutation to add invoice to order.
- **addOrderTag** → `Boolean!` — `input: UpdateOrderTagInput!`
- **approvePendingOrderTransactions** → `Boolean!` — `input: ApproveOrderTransactionsInput!`
- **bulkUpdateProducts** → `String!` — `input: [BulkUpdateProductsInput!]!`  
  Response indicating the status of operation.
- **campaignAddCoupons** → `[Coupon!]!` — `input: AddCouponsInput!`
- **cancelFulfillment** → `Order!` — `input: CancelFulfillmentInput!`  
  Use this mutation to cancel the already created package for an order.
- **cancelOrderLine** → `Order!` — `input: CancelOrderLineInput!`  
  Use this mutation to cancel given order lines.
- **changeStockLocation** → `Order!` — `input: ChangeStockLocationInput!`
- **createMerchantAppPayment** → `MerchantAppPayment!` — `input: MerchantAppPaymentInput!`  
  Using this api, you can add payment features to a merchant app.
- **createMerchantAppPaymentWithSubscription** → `MerchantAppPayment` — `input: CreateMerchantAppPaymentWithSubscriptionInput!`  
  Using this api, you can add payment features to a merchant app with app subscription id.
- **createOrderWithTransactions** → `Order!` — `input: CreateOrderWithTransactionsInput!`  
  Use this mutation if you want to create a new order with transactions.
- **createWalletTransaction** → `[IkasWalletTransaction!]!` — `input: CreateWalletTransactionInput!`  
  Using this API, you can make payments and refunds via Ikas Wallet.
- **deleteCampaignList** → `Boolean!` — `idList: [String!]!`  
  Mutation to delete Campaign
- **deleteCategoryList** → `Boolean!` — `idList: [String!]!`  
  Using this api, you can delete the categories of products.
- **deleteCouponList** → `String!` — `idList: [String!]!`
- **deleteCustomerGroupList** → `Boolean!` — `idList: [String!]!`  
  Mutation to delete CustomerGroup
- **deleteCustomerList** → `Boolean!` — `idList: [String!]!`  
  Mutation to delete Customer
- **deleteCustomerTagList** → `Boolean!` — `idList: [String!]!`  
  Mutation to delete CustomerTag
- **deleteGlobalTaxSettingsList** → `Boolean!` — `idList: [String!]!`  
  Mutation to delete GlobalTaxSettings
- **deleteOrderTagList** → `Boolean!` — `idList: [String!]!`  
  Mutation to delete OrderTag
- **deleteProductAttributeList** → `Boolean!` — `idList: [String!]!`  
  Use this mutation to delete product attributes with specific ids.
- **deleteProductBrandList** → `Boolean!` — `idList: [String!]!`  
  Using this api, you can delete the brands of products.
- **deleteProductList** → `Boolean!` — `idList: [String!]!`  
  Use this mutation to delete products with specific product ids.
- **deleteProductOrderList** → `Boolean!` — `idList: [String!]!`  
  Mutation to delete ProductOrder
- **deleteProductTagList** → `Boolean!` — `idList: [String!]!`  
  Using this api, you can delete the tags of products.
- **deleteProductUnitList** → `Boolean!` — `idList: [String!]!`  
  Using this api, you can delete the units of products.
- **deleteProductVolumeDiscountList** → `Boolean!` — `idList: [String!]!`  
  Using this api, you can delete the product volume discounts
- **deleteStorefrontJSScript** → `Boolean!` — `storefrontIdList: [String!]!`  
  Using this api, you can delete javascript script from a the storefront.
- **deleteStorefrontPolicyList** → `Boolean!` — `idList: [String!]!`  
  Using this API, you can delete a policy from the storefront.
- **deleteTaxSettingsList** → `Boolean!` — `idList: [String!]!`  
  Mutation to delete TaxSettings
- **deleteVariantTypeList** → `Boolean!` — `idList: [String!]!`  
  Using this api, you can delete the variant types of products.
- **deleteVendorList** → `Boolean!` — `idList: [String!]!`  
  Use this mutation to delete vendors with specific vendor ids.
- **deleteWebhook** → `Boolean!` — `scopes: [String!]!`  
  Use this mutation to delete webhooks by giving `scope` list.
- **fulfillOrder** → `Order!` — `input: FulFillOrderInput!`  
  Use this mutation to fulfill order line items.
- **generateOrderPaymentLink** → `String` — `input: GenerateOrderPaymentLinkInput!`
- **getAppDemoDay** → `Float` — `input: CreateMerchantAppPaymentWithSubscriptionInput!`  
  Using this api, you can collect demo day.
- **getOrderInvoicePdfUrl** → `String` — `input: GetOrderInvoicePdfUrlInput!`
- **refundOrderLine** → `Order!` — `input: OrderRefundInput!`  
  Use this mutation to refund given order lines.
- **removeOrderTag** → `Boolean!` — `input: UpdateOrderTagInput!`
- **saveCampaign** → `Campaign!` — `input: CampaignInput!`  
  Mutation to save Campaign
- **saveCategory** → `Category!` — `input: CategoryInput!`  
  Using this api, you can update the categories of products.
- **saveCustomer** → `Customer!` — `input: CustomerInput!`  
  Mutation to save Customer
- **saveCustomerGroup** → `CustomerGroup!` — `input: CustomerGroupInput!`  
  Mutation to save CustomerGroup
- **saveCustomerTag** → `CustomerTag!` — `input: CustomerTagInput!`  
  Mutation to save CustomerTag
- **saveGlobalTaxSettings** → `GlobalTaxSettings!` — `input: GlobalTaxSettingsInput!`  
  Mutation to save GlobalTaxSettings
- **saveOrderTag** → `OrderTag!` — `input: OrderTagInput!`  
  Mutation to save OrderTag
- **saveProduct** → `Product!` — `input: ProductInput!`  
  Use this mutation to create or update a product with provided input values.
- **saveProductAttribute** → `ProductAttribute!` — `input: ProductAttributeInput!`  
  Use this mutation to create or update product attributes with provided input values.
- **saveProductBrand** → `ProductBrand!` — `input: ProductBrandInput!`  
  Using this api, you can update the brands of products.
- **saveProductOrder** → `ProductOrder!` — `input: ProductOrderInput!`  
  Mutation to save ProductOrder
- **saveProductStockLocations** → `Boolean!` — `input: SaveStockLocationsInput!`  
  Use this mutation to define new stock or update stocks by location.
- **saveProductTag** → `ProductTag!` — `input: ProductTagInput!`  
  Using this api, you can update the tags of products.
- **saveProductUnit** → `ProductUnit!` — `input: ProductUnitInput!`  
  Using this api, you can update the units of products.
- **saveProductVolumeDiscount** → `ProductVolumeDiscount!` — `input: ProductVolumeDiscountInput!`  
  Using this api, you can update the product volume discounts
- **saveSalesChannel** → `SalesChannel` — `input: SalesChannelInput!`  
  Using this api you can update the sales channel name, priceList Id and stockLocations properties.
- **saveStorefrontJSScript** → `StorefrontJSScript!` — `input: StorefrontJSScriptInput!`  
  Using this api, you can save javascript script to a the storefront.
- **saveStorefrontPolicy** → `StorefrontPolicy!` — `input: StorefrontPolicyInput!`  
  Using this API, you can save a policy to the storefront.
- **saveTaxSettings** → `TaxSettings!` — `input: TaxSettingsInput!`  
  Mutation to save TaxSettings
- **saveVariantPrices** → `Boolean!` — `input: SaveVariantPricesInput!`  
  Response indicating the status of operation.
- **saveVariantType** → `VariantType!` — `input: VariantTypeInput!`  
  Using this api, you can update the variant types of products.
- **saveVendor** → `Vendor!` — `input: VendorInput!`  
  Use this mutation to define new vendor.
- **saveWebhook** → `[Webhook!]` — `input: WebhookInput!`  
  Use this mutation to save webhooks by using multiple `scope` variables. After saving a webhook, **ikas** will start to push new webhooks to given url `endpoint`. If **endpoint** is unreachable or returns an error code other than `HTTP 200` **ikas** will try to push webhook for 3 times then stops sending webhook.
- **updateCustomerB2BStatus** → `Boolean!` — `b2bStatus: CustomerB2BStatusEnum!`; `customerId: String!`
- **updateOrderAddresses** → `Order!` — `input: UpdateOrderAddressesInput!`  
  Use this mutation for order address information changes.
- **updateOrderLine** → `Order!` — `input: UpdateOrderInput!`  
  Use this mutation to cancel the fulfillment of the already created package for an order.
- **updateOrderPackageStatus** → `Order!` — `input: UpdateOrderPackageStatusInput!`  
  Use this mutation to update the status of packages linked to an order and the status of the order based on the status of those packages.
- **updateProductSalesChannelStatus** → `Boolean!` — `input: [UpdateProductSalesChannelStatusInput!]!`; `salesChannelId: String`  
  Response indicating the status of operation.
- **updateStoreCreditAmount** → `StoreCredit!` — `input: UpdateStoreCreditAmountInput!`  
  Set a store credit remaining balance. `amount` is the target remaining balance, not a new total. Used amount is unchanged. Sending the current remaining balance is a no-op.
- **updateSubscriptionStatus** → `Boolean!` — `customerId: String!`; `subscriptionStatus: CustomerEmailSubscriptionStatusesEnum!`

## Mutasyon girdi tipleri (zorunlu alanlar)

- **AddCouponsInput** — `campaignId`
- **AddOrderInvoiceInput** — `appId`, `invoiceNumber`, `orderId`, `sendNotificationToCustomer`, `type`
- **ApproveOrderTransactionsInput** — `orderId`
- **BulkUpdateProductBrandInput** — `name`
- **BulkUpdateProductCategoryInput** — `name`
- **BulkUpdateProductHTMLMetaDataInput** — `id`, `slug`
- **BulkUpdateProductHTMLMetaDataOverrideInput** — `description`, `pageTitle`
- **BulkUpdateProductImageInput** — `imageUrl`, `isMain`, `order`
- **BulkUpdateProductPriceInput** — `sellPrice`
- **BulkUpdateProductSalesChannels** — `id`
- **BulkUpdateProductStockInput** — `stockCount`, `stockLocationId`
- **BulkUpdateProductTagInput** — `name`
- **BulkUpdateProductVariantInput** — `deleted`, `isActive`, `prices`
- **BulkUpdateProductVariantTypeImportInput** — `order`, `variantValues`
- **BulkUpdateProductVariantValueImportInput** — `name`
- **BulkUpdateProductVariationValueRelationInput** — `variantTypeName`, `variantValueName`
- **BulkUpdateProductsInput** — `categories`, `deleted`, `name`, `productVariantTypes`, `type`, `variants`
- **BundleProductInput** — `filteredVariantIds`, `id`, `order`, `productId`, `quantity`
- **BundleProductOrderLineInput** — `bundleLineId`, `bundleLineQuantity`, `variant`
- **BundleProductOrderLineInputVariant** — `id`
- **BundleSettingsInput** — `products`
- **BuyXInput** — `amount`, `applyByQuantity`, `filter`
- **BuyXThenGetYInput** — `buyX`, `getY`
- **CampaignDateRangeFieldInput** — zorunlu alan yok
- **CampaignFilterInput** — `idList`, `type`
- **CampaignInput** — `applicablePrice`, `canCombineWithOtherCampaigns`, `hasCoupon`, `title`, `type`
- **CampaignMinMaxRangeFieldInput** — zorunlu alan yok
- **CampaignPeriodUsageLimitInput** — `maxUsage`, `scope`
- **CampaignTranslationInput** — `locale`
- **CancelFulfillmentInput** — `orderId`, `orderPackageId`
- **CancelFulfillmentOrderLineInput** — `orderLineItemId`, `quantity`
- **CancelOrderLineInput** — `orderId`, `orderLineItems`
- **CancelOrderLineItemInput** — `orderLineItemId`, `price`, `quantity`, `restockItems`
- **CategoryConditionInput** — `conditionType`, `valueList`
- **CategoryInput** — `name`
- **CategorySalesChannelInput** — `id`, `status`
- **CategoryTranslationInput** — `locale`
- **ChangeStockLocationInput** — `orderId`, `orderLineItemIds`, `targetStockLocationId`
- **CouponAutoAddProductInput** — `discountRatio`, `filter`, `quantity`
- **CreateMerchantAppPaymentWithSubscriptionInput** — `storeAppListingSubscriptionKey`
- **CreateOrderInput** — `orderLineItems`
- **CreateOrderWithTransactionsInput** — `order`, `transactions`
- **CreateSaleWithWalletOrderDetailInput** — `orderId`, `orderNumber`, `orderedDate`
- **CreateWalletTransactionInput** — `force`, `name`, `price`, `transactionCategory`, `transactionId`, `type`
- **CustomerAddressCityInput** — `name`
- **CustomerAddressCountryInput** — `name`
- **CustomerAddressDistrictInput** — zorunlu alan yok
- **CustomerAddressInput** — `addressLine1`, `city`, `country`, `firstName`, `isDefault`, `lastName`, `postalCode`, `title`
- **CustomerAddressRegionInput** — `id`, `name`
- **CustomerAddressStateInput** — zorunlu alan yok
- **CustomerAttributeValueInput** — zorunlu alan yok
- **CustomerGroupConditionInput** — `conditionType`, `valueList`
- **CustomerGroupInput** — `name`
- **CustomerInput** — `firstName`
- **CustomerPriceListRuleFilterInput** — `type`, `valueList`
- **CustomerPriceListRuleInput** — `value`, `valueType`
- **CustomerTagInput** — `name`
- **FixedDiscountInput** — zorunlu alan yok
- **FulFillOrderInput** — `lines`, `orderId`
- **FulfillOrderLineInput** — `orderLineItemId`, `quantity`
- **GenerateCouponInput** — `canCombineWithOtherCampaigns`, `prefix`, `quantity`
- **GenerateOrderPaymentLinkInput** — `orderId`
- **GetOrderInvoicePdfUrlInput** — `invoiceId`, `orderId`
- **GetYInput** — `amount`, `discountRatio`, `filter`
- **GlobalTaxSettingsInput** — `isTaxExcludedOnPrices`, `isTaxExcludedOnShippingPrices`
- **HTMLMetaDataInput** — `slug`
- **HTMLMetaDataOverrideInput** — `description`, `pageTitle`
- **HTMLMetaDataTranslationInput** — `locale`
- **MerchantAppPaymentInput** — `name`, `price`
- **NewCouponInput** — `canCombineWithOtherCampaigns`, `code`
- **OrderAddressCityInput** — `name`
- **OrderAddressCountryInput** — `name`
- **OrderAddressDistrictInput** — zorunlu alan yok
- **OrderAddressInput** — `addressLine1`, `city`, `country`, `firstName`, `isDefault`, `lastName`
- **OrderAddressRegionInput** — `id`, `name`
- **OrderAddressStateInput** — zorunlu alan yok
- **OrderAdjustmentInput** — `amount`, `amountType`, `name`, `order`, `type`
- **OrderCustomerInput** — zorunlu alan yok
- **OrderLineDiscountInput** — `amount`, `amountType`
- **OrderLineItemInput** — `price`, `quantity`, `variant`
- **OrderLineOptionInput** — `productOptionId`, `productOptionsSetId`, `values`
- **OrderLineOptionValueInput** — `value`
- **OrderLineVariantBundleProductInput** — `id`, `order`, `productId`, `quantity`, `variant`
- **OrderLineVariantInput** — zorunlu alan yok
- **OrderRefundBranchInfoInput** — `branchSessionId`, `terminalId`
- **OrderRefundInput** — `orderId`, `orderRefundLines`, `stockLocationId`
- **OrderRefundLineInput** — `orderLineItemId`, `price`, `quantity`, `restockItems`
- **OrderRefundRejectedLineInput** — `orderLineItemId`, `quantity`
- **OrderRefundTransactionInput** — `amount`, `transactionId`
- **OrderShippingLineInput** — `price`, `title`
- **OrderStaffInput** — `email`, `firstName`, `id`, `lastName`
- **OrderTagInput** — `name`
- **OrderTransactionInput** — `amount`
- **ProductAttributeInput** — `name`, `type`
- **ProductAttributeOptionInput** — `name`
- **ProductAttributeOptionTranslationInput** — `id`
- **ProductAttributeTableCellDataInput** — `id`, `name`
- **ProductAttributeTableTemplateInput** — `columns`, `rows`
- **ProductAttributeTranslationInput** — `locale`
- **ProductAttributeValueInput** — `productAttributeId`
- **ProductBaseUnitModelInput** — `baseAmount`, `type`
- **ProductBrandInput** — `name`
- **ProductBrandTranslationInput** — `locale`
- **ProductImageInput** — `imageId`, `isMain`, `order`
- **ProductInput** — `name`, `type`, `variants`
- **ProductOrderInput** — `products`
- **ProductOrderProductInput** — `order`, `productId`
- **ProductPriceInput** — `sellPrice`
- **ProductSalesChannelInput** — `id`, `status`
- **ProductStockLocationInput** — `productId`, `stockCount`, `stockLocationId`, `variantId`
- **ProductTagInput** — `name`
- **ProductTagTranslationInput** — `locale`
- **ProductTranslationInput** — `locale`
- **ProductUnitInput** — `name`
- **ProductUnitTranslationInput** — `locale`
- **ProductVariantTypeInput** — `order`, `variantTypeId`, `variantValueIds`
- **ProductVolumeDiscountInput** — `name`, `rules`, `scope`
- **ProductVolumeDiscountRuleInput** — `discountRatio`, `lineItemQuantityRange`
- **SalesChannelInput** — `stockLocations`
- **SalesChannelStockLocationInput** — `id`, `order`
- **SaveStockLocationsInput** — zorunlu alan yok
- **SaveVariantPricesInput** — `variantPriceInputs`
- **StorefrontJSScriptInput** — `contentType`, `name`, `scriptContent`, `storefrontId`
- **StorefrontPolicyInput** — `content`, `locale`, `name`, `sendEmail`, `slug`, `storefrontId`, `storefrontRoutings`, `type`
- **StorefrontPolicyRoutingInput** — `storefrontId`, `storefrontRoutingId`
- **TaxSettingsGiftPackageTaxRateInput** — `taxRate`
- **TaxSettingsInput** — `countryId`, `productOverrides`, `taxRate`
- **TaxSettingsProductOverrideInput** — `productTagIds`, `taxRate`
- **TaxSettingsRateInput** — `stateId`, `taxRate`
- **TaxSettingsShippingTaxRateInput** — `taxRate`
- **TieredDiscountInput** — `rules`
- **TieredDiscountRuleInput** — `amount`
- **TimelineInput** — `message`, `sourceId`, `sourceType`
- **TrackingInfoDetailInput** — zorunlu alan yok
- **UpdateOrderAddressesInput** — `orderId`
- **UpdateOrderInput** — `editReason`, `orderId`, `orderLineItems`, `restockItems`
- **UpdateOrderPackageStatusInput** — `orderId`, `packages`
- **UpdateOrderPackageStatusPackagesInput** — `packageId`, `status`
- **UpdateOrderTagInput** — `orderId`, `orderTagId`
- **UpdateProductSalesChannelStatusInput** — `active`, `productId`
- **UpdateStoreCreditAmountInput** — `amount`, `id`
- **VariantInput** — `isActive`, `prices`
- **VariantPriceInput** — `price`, `productId`, `variantId`
- **VariantTypeInput** — `name`, `selectionType`, `values`
- **VariantTypeTranslationInput** — `locale`
- **VariantUnitModelInput** — `amount`, `type`
- **VariantValueInput** — `name`
- **VariantValueRelationInput** — `variantTypeId`, `variantValueId`
- **VariantValueTranslationInput** — `id`
- **VendorInput** — `name`
- **VolumeDiscountMinMaxRangeFieldInput** — `min`
- **WebhookInput** — `endpoint`, `scopes`

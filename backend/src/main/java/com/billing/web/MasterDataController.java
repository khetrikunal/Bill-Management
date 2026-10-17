package com.billing.web;

import com.billing.domain.Customer;
import com.billing.domain.Product;
import com.billing.dto.Dtos.*;
import com.billing.repo.CustomerRepository;
import com.billing.repo.ProductRepository;
import com.billing.service.ApiException;
import com.billing.service.ImportService;
<<<<<<< HEAD
import com.billing.service.CompanyAccessService;
=======
>>>>>>> origin/main
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
<<<<<<< HEAD
import org.springframework.security.core.Authentication;
=======
>>>>>>> origin/main
import java.io.IOException;
import java.util.List;

/** Buyers (customers) and the product list used to speed up bill entry. */
@RestController
@RequestMapping("/api")
public class MasterDataController {
    private static final String WRITE = "hasAnyRole('ADMIN','BILLING')";
    private final CustomerRepository customers;
    private final ProductRepository products;
    private final ImportService importer;
<<<<<<< HEAD
    private final CompanyAccessService companyAccess;

    public MasterDataController(CustomerRepository customers, ProductRepository products, ImportService importer,
                                CompanyAccessService companyAccess) {
        this.customers = customers;
        this.products = products;
        this.importer = importer;
        this.companyAccess = companyAccess;
=======

    public MasterDataController(CustomerRepository customers, ProductRepository products, ImportService importer) {
        this.customers = customers;
        this.products = products;
        this.importer = importer;
>>>>>>> origin/main
    }

    // ---- customers
    @GetMapping("/customers")
<<<<<<< HEAD
    public List<CustomerDto> listCustomers(@RequestParam(defaultValue = "") String q,
                                           @RequestHeader(value = "X-Company-Id", defaultValue = "1") Long companyId,
                                           Authentication authentication) {
        companyAccess.requireAccess(authentication.getName(), companyId);
        return customers.findTop200ByCompanyIdAndNameContainingIgnoreCaseOrderByName(companyId, q)
                .stream().map(CustomerDto::of).toList();
=======
    public List<CustomerDto> listCustomers(@RequestParam(defaultValue = "") String q) {
        return customers.findTop200ByNameContainingIgnoreCaseOrderByName(q).stream().map(CustomerDto::of).toList();
>>>>>>> origin/main
    }

    @PostMapping("/customers")
    @PreAuthorize(WRITE)
<<<<<<< HEAD
    public CustomerDto createCustomer(@Valid @RequestBody CustomerDto d,
                                      @RequestHeader(value = "X-Company-Id", defaultValue = "1") Long companyId,
                                      Authentication authentication) {
        companyAccess.requireAccess(authentication.getName(), companyId);
        return CustomerDto.of(customers.save(apply(new Customer(), d, companyId)));
    }
=======
    public CustomerDto createCustomer(@Valid @RequestBody CustomerDto d) { return CustomerDto.of(customers.save(apply(new Customer(), d))); }
>>>>>>> origin/main

    @PutMapping("/customers/{id}")
    @PreAuthorize(WRITE)
    @Transactional
<<<<<<< HEAD
    public CustomerDto updateCustomer(@PathVariable Long id, @Valid @RequestBody CustomerDto d,
                                      @RequestHeader(value = "X-Company-Id", defaultValue = "1") Long companyId,
                                      Authentication authentication) {
        companyAccess.requireAccess(authentication.getName(), companyId);
        Customer c = customers.findById(id).orElseThrow(() -> ApiException.notFound("Customer not found"));
        if (!companyId.equals(c.companyId)) throw ApiException.notFound("Customer not found");
        return CustomerDto.of(customers.save(apply(c, d, companyId)));
=======
    public CustomerDto updateCustomer(@PathVariable Long id, @Valid @RequestBody CustomerDto d) {
        Customer c = customers.findById(id).orElseThrow(() -> ApiException.notFound("Customer not found"));
        return CustomerDto.of(customers.save(apply(c, d)));
>>>>>>> origin/main
    }

    @DeleteMapping("/customers/{id}")
    @PreAuthorize(WRITE)
<<<<<<< HEAD
    public void deleteCustomer(@PathVariable Long id,
                               @RequestHeader(value = "X-Company-Id", defaultValue = "1") Long companyId,
                               Authentication authentication) {
        companyAccess.requireAccess(authentication.getName(), companyId);
        if (customers.deleteByIdAndCompanyId(id, companyId) == 0) throw ApiException.notFound("Customer not found");
    }

    private static Customer apply(Customer c, CustomerDto d, Long companyId) {
        c.companyId = companyId;
=======
    public void deleteCustomer(@PathVariable Long id) { customers.deleteById(id); }

    private static Customer apply(Customer c, CustomerDto d) {
>>>>>>> origin/main
        c.name = d.name().trim();
        c.address = d.address() == null ? "" : d.address().trim();
        c.gstin = d.gstin() == null ? "" : d.gstin().trim();
        c.email = d.email() == null ? "" : d.email().trim();
        c.phone = d.phone() == null ? "" : d.phone().trim();
        return c;
    }

    // ---- products
    @GetMapping("/products")
<<<<<<< HEAD
    public List<ProductDto> listProducts(@RequestParam(defaultValue = "") String q,
                                         @RequestHeader(value = "X-Company-Id", defaultValue = "1") Long companyId,
                                         Authentication authentication) {
        companyAccess.requireAccess(authentication.getName(), companyId);
        return products.findTop500ByCompanyIdAndNameContainingIgnoreCaseOrderByName(companyId, q)
                .stream().map(ProductDto::of).toList();
=======
    public List<ProductDto> listProducts(@RequestParam(defaultValue = "") String q) {
        return products.findTop500ByNameContainingIgnoreCaseOrderByName(q).stream().map(ProductDto::of).toList();
>>>>>>> origin/main
    }

    @PostMapping("/products")
    @PreAuthorize(WRITE)
<<<<<<< HEAD
    public ProductDto createProduct(@Valid @RequestBody ProductDto d,
                                    @RequestHeader(value = "X-Company-Id", defaultValue = "1") Long companyId,
                                    Authentication authentication) {
        companyAccess.requireAccess(authentication.getName(), companyId);
        return ProductDto.of(products.save(apply(new Product(), d, companyId)));
    }
=======
    public ProductDto createProduct(@Valid @RequestBody ProductDto d) { return ProductDto.of(products.save(apply(new Product(), d))); }
>>>>>>> origin/main

    @PutMapping("/products/{id}")
    @PreAuthorize(WRITE)
    @Transactional
<<<<<<< HEAD
    public ProductDto updateProduct(@PathVariable Long id, @Valid @RequestBody ProductDto d,
                                    @RequestHeader(value = "X-Company-Id", defaultValue = "1") Long companyId,
                                    Authentication authentication) {
        companyAccess.requireAccess(authentication.getName(), companyId);
        Product p = products.findById(id).orElseThrow(() -> ApiException.notFound("Product not found"));
        if (!companyId.equals(p.companyId)) throw ApiException.notFound("Product not found");
        return ProductDto.of(products.save(apply(p, d, companyId)));
=======
    public ProductDto updateProduct(@PathVariable Long id, @Valid @RequestBody ProductDto d) {
        Product p = products.findById(id).orElseThrow(() -> ApiException.notFound("Product not found"));
        return ProductDto.of(products.save(apply(p, d)));
>>>>>>> origin/main
    }

    @DeleteMapping("/products/{id}")
    @PreAuthorize(WRITE)
<<<<<<< HEAD
    public void deleteProduct(@PathVariable Long id,
                              @RequestHeader(value = "X-Company-Id", defaultValue = "1") Long companyId,
                              Authentication authentication) {
        companyAccess.requireAccess(authentication.getName(), companyId);
        if (products.deleteByIdAndCompanyId(id, companyId) == 0) throw ApiException.notFound("Product not found");
    }

    private static Product apply(Product p, ProductDto d, Long companyId) {
        p.companyId = companyId;
=======
    public void deleteProduct(@PathVariable Long id) { products.deleteById(id); }

    private static Product apply(Product p, ProductDto d) {
>>>>>>> origin/main
        p.name = d.name().trim();
        p.itemCode = d.itemCode() == null ? "" : d.itemCode().trim();
        p.hsn = d.hsn() == null ? "" : d.hsn().trim();
        p.unit = d.unit().trim();
        p.rate = d.rate();
        p.cgstRate = d.cgstRate();
        p.sgstRate = d.sgstRate();
        p.active = d.active();
        return p;
    }

    /** commit=false returns a preview only; commit=true saves the NEW rows. */
    @PostMapping("/products/import")
    @PreAuthorize(WRITE)
    public ImportResult importProducts(@RequestParam("file") MultipartFile file,
                                       @RequestParam(defaultValue = "table") String mode,
                                       @RequestParam(required = false) String sheet,
<<<<<<< HEAD
                                       @RequestParam(defaultValue = "false") boolean commit,
                                       @RequestHeader(value = "X-Company-Id", defaultValue = "1") Long companyId,
                                       Authentication authentication) throws IOException {
        companyAccess.requireAccess(authentication.getName(), companyId);
        if (file.isEmpty() || file.getSize() > 5_000_000) throw ApiException.badRequest("Upload an .xlsx file under 5 MB.");
        return importer.run(companyId, file.getInputStream(), mode, sheet, commit);
=======
                                       @RequestParam(defaultValue = "false") boolean commit) throws IOException {
        if (file.isEmpty() || file.getSize() > 5_000_000) throw ApiException.badRequest("Upload an .xlsx file under 5 MB.");
        return importer.run(file.getInputStream(), mode, sheet, commit);
>>>>>>> origin/main
    }
}

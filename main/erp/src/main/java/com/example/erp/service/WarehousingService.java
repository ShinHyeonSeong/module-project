package com.example.erp.service;

import com.example.erp.dto.*;
import com.example.erp.entity.*;
import com.example.erp.repository.*;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
public class WarehousingService {

    private final StorageRepository storageRepository;
    private final SectionRepository sectionRepository;
    private final ProductRepository productRepository;
    private final PartRepository partRepository;
    private final NewStockRepository newStockRepository;
    private final ArrivalCityRepository arrivalCityRepository;

    @Autowired
    public WarehousingService(StorageRepository storageRepository,
                              SectionRepository sectionRepository,
                              ProductRepository productRepository,
                              PartRepository partRepository,
                              NewStockRepository newStockRepository,
                              ArrivalCityRepository arrivalCityRepository) {
        this.storageRepository = storageRepository;
        this.sectionRepository = sectionRepository;
        this.productRepository = productRepository;
        this.partRepository = partRepository;
        this.newStockRepository = newStockRepository;
        this.arrivalCityRepository = arrivalCityRepository;
    }

    Date date = new Date();
    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    // *! 해당 메서드들은 위치해야할 클래스에 배치되지 않은 경우가 다수 있음. 추후 리팩토링 필요할것


    // 새로운 입고 물품 신청
    @Transactional
    public NewStockDto createNewStock(long storageId, long productId, int count) {

        // 반환 Optional 이딴식으로 하면 메서드 종료 후 어딘가에서 반드시 try catch 예외 처리를 받아야 할텐데
        // 이게 맞나
        Storage storage = storageRepository.findById(storageId)
                .orElseThrow(() -> new IllegalArgumentException("스토리지 없당"));

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("제품 없당"));

        NewStock newStock = NewStock.builder()
                .storage(storage)
                .product(product)
                .count(count)
                .stockDate(date)
                .build();

        NewStockDto newStockDto = new NewStockDto();
        newStockDto.toDto(newStockRepository.save(newStock));
        return newStockDto;
    }

    // 파라미터의 스토리지에 금일 보관되는 product 및 개수
    public List<NewStockDto> countInStockInStorage(long storageId) {
        return newStockRepository.findAllByStorage(storageId).stream()
                .filter(newStock -> newStock.getStockDate() == date)
                .map(NewStockDto::new)
                .collect(Collectors.toList());
    }

    // part 생성 메서드
    public PartDto inStock(ProductDto productDto, StorageDto storageDto) {

        if (productDto == null) throw new NoSuchElementException("no product exist");
        if (storageDto.getState() == 1) throw new IllegalArgumentException("full storage capacity");

        int productSize = Math.round(productDto.getSize());
        int sectionNum = calcSection(productSize);


        // select section by storage && sectionNumber. 추후 분리가 필요할 수도 있음.
        Section section = sectionRepository.findByStorageAndSectionNumber(storageDto.getStorageId(), sectionNum);
        if (!isSectionCapacityLeft(section)) throw new IllegalArgumentException("full section capacity");

        Part part = Part.builder()
                .section(section)
                .product(ProductDto.toEntity(productDto))
                .startStock(date)
                .build();

        PartDto partDto = PartDto.toDto(partRepository.save(part));
        increaseSectionCapacity(SectionDto.toDto(section));

        return partDto;
    }

    // product size를 바탕으로 section을 할당해주는 계산 메서드. 가시성을 위해 분리함
    public int calcSection(int productSize) {
        int sectionNum = 0;

        if (productSize < 0) throw new IllegalArgumentException("negative product size");

        if (productSize == 0) {
            sectionNum = 6;
            return sectionNum;
        }

        if (productSize > 0 && productSize >= 20) {
            sectionNum = 1;
        } else if (productSize > 20 && productSize <= 50) {
            sectionNum = 2;
            return sectionNum;
        } else if (productSize > 50 && productSize <= 100) {
            sectionNum = 3;
        }  else if (productSize > 100 && productSize <= 200) {
            sectionNum = 4;
        } else if (productSize > 200) {
            sectionNum = 5;
        }

        return sectionNum;
    }

    // section capacity 계산. 공간이 남을 시 true, 아니면 false 반환
    // 추후 사용처가 많지 않을 경우 메서드 병합 고려.
    public boolean isSectionCapacityLeft(Section section) {
        if (section.getCapacity() > section.getCurrentCapacity()) return true;
        return false;
    }

    // Section이 가득 차있지 않으면 Current Capacity를 +1. 이후 checkChildSection 메서드를 호출함
    public void increaseSectionCapacity(SectionDto sectionDto) {

        // 변환 과정이 너무 난잡함. 리팩토링 필요할듯
        // select 메서드가 Dto를 반환함을 전제로 아래 SectionDto의 변환 과정을 살펴볼것.
        // 1. SectionDto -> Section 변환을 위해 매개변수로 Storage가 필요함.
        // 2. repository를 통해 StorageDto 탐색
        // 3. StorageDto -> Storage 변환을 위해 매개변수로 ArrivalCity가 필요함.
        // 4. repository를 통해 ArrivalCityDto 탐색
        // 5. ArrivalCityDto -> ArrivalCity 변환
        // 6. StorageDto.toEntity(ArrivalCity) -> Storage 변환
        // 7. SectionDto.toEntity(Storage) -> Section 변환

        // Entity 타입을 반환하는 select 메서드를 추가하거나, 변환을 따로 처리할 필요가 있어 보임

        StorageDto storageDto = findStorageById(sectionDto.getStorageId());
        ArrivalCityDto arrivalCityDto = findArrivalCityById(storageDto.getArrivalCity());
        Storage storage = storageDto.toEntity(arrivalCityDto.toEntity());

        if (!isSectionCapacityLeft(sectionDto.toEntity(storage)))
            throw new IllegalArgumentException("full section capacity");
        else {
            sectionDto.setCurrentCapacity(sectionDto.getCurrentCapacity() + 1);
            sectionRepository.save(sectionDto.toEntity(storage));

            checkChildSection(storageDto);
        }
    }

    // storage의 자식 section의 current capacity를 검사하여 storage의 state 조정
    // 가득 차면 1, 아니면 0
    public void checkChildSection(StorageDto storageDto) {
        int storageState;

        List<Section> sectionList = sectionRepository.findAll()
                .stream()
                .filter(section -> section.getStorage().getStorageId() == storageDto.getStorageId())
                .filter(section -> isSectionCapacityLeft(section))
                .collect(Collectors.toList());

        if (sectionList.isEmpty()) storageState = 1;
        else storageState = 0;
        storageDto.setState(storageState);

        ArrivalCity arrivalCity = findArrivalCityById(storageDto.getArrivalCity()).toEntity();
        storageRepository.save(storageDto.toEntity(arrivalCity));
    }


    // !** select 메서드. 위치하는 클래스가 추후 달라져야할것임. **!

    public ArrivalCityDto findArrivalCityById(long arrivalCityId) {
        ArrivalCity arrivalCity = arrivalCityRepository.findById(arrivalCityId)
                .orElseThrow(() -> new IllegalArgumentException("no arrivalCity exist"));

        ArrivalCityDto arrivalCityDto = ArrivalCityDto.toDto(arrivalCity);
        return arrivalCityDto;
    }

    public StorageDto findStorageById(long storageId) {
        Storage storage = storageRepository.findById(storageId)
                .orElseThrow(() -> new IllegalArgumentException("no storage exist"));

        StorageDto storageDto = StorageDto.toDto(storage);
        return storageDto;
    }

    public SectionDto findSectionById(long sectionId) {
        Section section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new IllegalArgumentException("no section exist"));

        SectionDto sectionDto = SectionDto.toDto(section);
        return sectionDto;
    }

}

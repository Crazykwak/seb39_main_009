package teamparkinglot.parkinggo.notice.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.web.PageableDefault;
import teamparkinglot.parkinggo.notice.dto.NoticeResDto;
import teamparkinglot.parkinggo.notice.entity.Notice;

import java.util.List;

public interface NoticeRepository extends JpaRepository<Notice, Long> {

    @Query("select new teamparkinglot.parkinggo.notice.dto.NoticeResDto(n.type, n.title, n.body, n.createdDate, n.modifiedDate) from Notice n where n.inUse = true")
    Page<NoticeResDto> findNoticeAll(Pageable pageable);
}

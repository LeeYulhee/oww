package flobitt.oww.domain.workout.entity;

import flobitt.oww.domain.base.entity.BaseEntity;
import flobitt.oww.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Entity
@Table(name = "WORKOUT_RECORDS")
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class WorkoutRecord extends BaseEntity {

    @Id
    @EqualsAndHashCode.Include
    @UuidGenerator(style = UuidGenerator.Style.RANDOM)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "record_id", columnDefinition = "VARCHAR(36)")
    private UUID id;

    @Column(name = "workout_date")
    private LocalDate workoutDate;

    @Column(name = "workout_memo")
    private String workoutMemo;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "duration_minutes")
    private int durationMinutes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
}

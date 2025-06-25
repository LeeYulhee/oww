package flobitt.oww.domain.notification.entity;

import flobitt.oww.domain.base.entity.BaseEntity;
import flobitt.oww.domain.group.entity.Group;
import flobitt.oww.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Getter
@Entity
@Table(name = "NOTIFICATION_SETTINGS")
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class NotificationSetting extends BaseEntity {

    @Id
    @EqualsAndHashCode.Include
    @UuidGenerator(style = UuidGenerator.Style.RANDOM)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "setting_id", columnDefinition = "VARCHAR(36)")
    private UUID id;

    @Column(name = "workout_reminder")
    private Boolean workoutReminder;

    @Column(name = "group_member_workout")
    private Boolean groupMemberWorkout;

    @Column(name = "weekly_report")
    private Boolean weeklyReport;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    private Group group;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
}

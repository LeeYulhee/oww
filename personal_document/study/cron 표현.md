```yaml
app:
scheduler:
user-cleanup-cron: "*/30 * * * * *"  # 30초마다
```

**🕐 cron 표현식 구조**
```yaml
초 분 시 일 월 요일
```

**📝 예시들**
```yaml
# 30초마다
user-cleanup-cron: "*/30 * * * * *"

# 10초마다
user-cleanup-cron: "*/10 * * * * *"

# 5초마다
user-cleanup-cron: "*/5 * * * * *"

# 매분 0초에 (1분마다)
user-cleanup-cron: "0 * * * * *"

# 매분 30초에 (1분마다, 30초 시점)
user-cleanup-cron: "30 * * * * *"
```

**🔍 구체적인 차이점**<br>
기존 (10분마다):
```yaml
yamluser-cleanup-cron: "0 */10 * * * *"
#                   초 분   시 일 월 요일
#                   0  10분마다
```

새로운 (30초마다):
```yaml
yamluser-cleanup-cron: "*/30 * * * * *"
#                   초     분 시 일 월 요일
#                   30초마다
```

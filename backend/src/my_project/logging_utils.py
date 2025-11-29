import logging

# ロガーで
# %(name)s の代わりに %(short_name)s と使えるやつ
#
# root_logger = logging.getLogger()
# for handler in root_logger.handlers:
#     handler.addFilter(PackagePathFilter())
# みたいにフィルタを追加し(これでいいのか？？)、フォーマットの設定のところに %(short_name)s と指定する。


# 1. ロガー名を短縮するフィルタを定義
class PackagePathFilter(logging.Filter):
    def filter(self, record: logging.LogRecord) -> bool:
        # ドットで分割
        parts = record.name.split(".")
        if len(parts) > 1:
            # 最後の要素以外を頭文字だけにする (例: my.package.module -> m.p.module)
            short_parts = [p[0] for p in parts[:-1]] + [parts[-1]]
            record.short_name = ".".join(short_parts)
        else:
            record.short_name = record.name
        return True

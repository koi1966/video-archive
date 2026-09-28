const CHUNK_SIZE = 8 * 1024 * 1024;

/*
 * Загрузка видеофайлов chunks по 8 MiB.
 *    .
 * Для ADMIN рабочая зона выбирается на странице записи
 * и передаётся в каждом запросе как workAreaId.
 *   .
 * Для обычного пользователя workAreaId можно не передавать:
 * Controller сам возьмёт рабочую зону из текущего пользователя.
 */

document.addEventListener('DOMContentLoaded', function () {

    const input = document.getElementById('videoFiles');
    const button = document.getElementById('uploadButton');
    const progress = document.getElementById('uploadProgress');
    const bar = document.getElementById('uploadBar');
    const status = document.getElementById('uploadStatus');
    const workAreaSelect = document.getElementById('workAreaId');

    if (!input || !button || !progress || !bar || !status) {
        console.error('Не знайдено елементів завантаження відео:', {
            input,
            button,
            progress,
            bar,
            status
        });
        return;
    }

    console.log('upload.js загружен');
    console.log('Текущий URL:', window.location.pathname);


    /*
     * Получаем recordId из URL.
     *
     * Например:
     * /records/6ab8fee7b2d7da2a8a204a66
     */
    function getRecordId() {

        const parts = window.location.pathname
            .split('/')
            .filter(part => part.length > 0);

        const recordsIndex = parts.indexOf('records');

        if (recordsIndex === -1 ||
            recordsIndex + 1 >= parts.length) {

            return null;
        }

        return parts[recordsIndex + 1];
    }


    button.addEventListener('click', async function () {

        console.log('Кнопка загрузки нажата');

        const recordId = getRecordId();

        console.log('recordId:', recordId);


        if (!recordId) {

            status.textContent =
                'Помилка: не вдалося визначити ID запису.';

            console.error(
                'Не вдалося визначити recordId з URL:',
                window.location.pathname
            );

            return;
        }


        if (!input.files ||
            input.files.length === 0) {

            status.textContent =
                'Виберіть хоча б один відеофайл.';

            return;
        }


        /*
         * Если на странице есть select workAreaId,
         * значит это ADMIN.
         *
         * Для ADMIN выбор обязателен.
         */
        let workAreaId = null;

        if (workAreaSelect) {

            workAreaId =
                workAreaSelect.value;

            if (!workAreaId) {

                status.textContent =
                    'Оберіть робочу зону перед копіюванням.';

                workAreaSelect.focus();

                return;
            }
        }


        /*
         * CSRF.
         */
        const csrfTokenElement =
            document.querySelector('meta[name="_csrf"]');

        const csrfHeaderElement =
            document.querySelector('meta[name="_csrf_header"]');

        const csrfToken =
            csrfTokenElement
                ? csrfTokenElement.content
                : null;

        const csrfHeader =
            csrfHeaderElement
                ? csrfHeaderElement.content
                : null;


        console.log(
            'CSRF token найден:',
            !!csrfToken
        );

        console.log(
            'CSRF header:',
            csrfHeader
        );

        console.log(
            'workAreaId:',
            workAreaId
        );


        button.disabled = true;

        progress.hidden = false;

        bar.value = 0;

        status.textContent =
            'Починаємо завантаження...';


        try {

            /*
             * Загружаем файлы один за другим.
             */
            for (
                let fileIndex = 0;
                fileIndex < input.files.length;
                fileIndex++
            ) {

                const file =
                    input.files[fileIndex];


                console.log(
                    'Начинаем загрузку файла:',
                    file.name,
                    'размер:',
                    file.size
                );


                /*
                 * Один uploadId на один файл.
                 *
                 * Все chunks этого файла
                 * используют один uploadId.
                 */
                const uploadId =
                    crypto.randomUUID();


                const totalChunks =
                    Math.ceil(
                        file.size / CHUNK_SIZE
                    );


                console.log(
                    'uploadId:',
                    uploadId,
                    'totalChunks:',
                    totalChunks
                );


                /*
                 * Chunks одного файла отправляем
                 * последовательно.
                 */
                for (
                    let chunkNumber = 0;
                    chunkNumber < totalChunks;
                    chunkNumber++
                ) {

                    const start =
                        chunkNumber * CHUNK_SIZE;

                    const end =
                        Math.min(
                            start + CHUNK_SIZE,
                            file.size
                        );

                    const chunk =
                        file.slice(
                            start,
                            end
                        );


                    const form =
                        new FormData();


                    form.append(
                        'uploadId',
                        uploadId
                    );

                    form.append(
                        'fileName',
                        file.name
                    );

                    form.append(
                        'totalSize',
                        String(file.size)
                    );

                    form.append(
                        'chunkNumber',
                        String(chunkNumber)
                    );

                    form.append(
                        'totalChunks',
                        String(totalChunks)
                    );

                    /*
                     * Для ADMIN передаём выбранную
                     * рабочую зону.
                     *
                     * Для обычного пользователя
                     * поле не добавляем: Controller
                     * возьмёт user.workAreaId.
                     */
                    if (workAreaId) {

                        form.append(
                            'workAreaId',
                            workAreaId
                        );
                    }


                    form.append(
                        'chunk',
                        chunk,
                        file.name + '.part'
                    );


                    const headers = {};


                    if (
                        csrfToken &&
                        csrfHeader
                    ) {

                        headers[csrfHeader] =
                            csrfToken;
                    }


                    const url =
                        `/records/${recordId}/videos/chunk`;


                    console.log(
                        'Отправляем chunk:',
                        chunkNumber + 1,
                        '/',
                        totalChunks,
                        'размер:',
                        chunk.size,
                        'workAreaId:',
                        workAreaId
                    );


                    const response =
                        await fetch(
                            url,
                            {
                                method: 'POST',
                                headers: headers,
                                body: form
                            }
                        );


                    console.log(
                        'Ответ сервера:',
                        response.status
                    );


                    if (!response.ok) {

                        let errorText = '';

                        try {

                            errorText =
                                await response.text();

                        } catch (e) {

                            errorText = '';
                        }


                        throw new Error(
                            `HTTP ${response.status}` +
                            (
                                errorText
                                    ? `: ${errorText}`
                                    : ''
                            )
                        );
                    }


                    const percent =
                        Math.round(
                            (end / file.size) * 100
                        );


                    bar.value =
                        percent;


                    status.textContent =
                        `Файл ${fileIndex + 1} из ` +
                        `${input.files.length}: ` +
                        `${file.name} — ${percent}%`;


                    console.log(
                        `Файл ${file.name}: ` +
                        `chunk ${chunkNumber + 1}/` +
                        `${totalChunks}, ` +
                        `${percent}%`
                    );
                }


                console.log(
                    'Файл полностью загружен:',
                    file.name
                );
            }


            bar.value = 100;


            status.textContent =
                'Усі файли успішно скопійовані на відеодиск.';


            console.log(
                'Завантаження всіх файлів завершено'
            );


            setTimeout(
                function () {
                    location.reload();
                },
                800
            );


        } catch (error) {

            console.error(
                'Ошибка загрузки:',
                error
            );


            status.textContent =
                'Ошибка копирования: ' +
                error.message;


            button.disabled = false;
        }
    });
});


const CHUNK_SIZE = 8 * 1024 * 1024;

/*
 * Загрузка видеофайлов большими chunks.
 *
 * ID записи берём непосредственно из URL:
 * /records/6ab25499bdb64082cbdc3bcd
 *
 * Поэтому Thymeleaf [[${record.id}]] здесь не нужен.
 */

document.addEventListener('DOMContentLoaded', function () {

    const input = document.getElementById('videoFiles');
    const button = document.getElementById('uploadButton');
    const progress = document.getElementById('uploadProgress');
    const bar = document.getElementById('uploadBar');
    const status = document.getElementById('uploadStatus');

    if (!input || !button || !progress || !bar || !status) {
        console.error('Не найдены элементы загрузки видео:', {
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
     * /records/6ab25499bdb64082cbdc3bcd
     *
     * Получаем:
     * 6ab25499bdb64082cbdc3bcd
     */
    function getRecordId() {

        const parts = window.location.pathname
            .split('/')
            .filter(part => part.length > 0);

        const recordsIndex = parts.indexOf('records');

        if (recordsIndex === -1 || recordsIndex + 1 >= parts.length) {
            return null;
        }

        return parts[recordsIndex + 1];
    }


    /*
     * Обработчик кнопки.
     */
    button.addEventListener('click', async function () {

        console.log('Кнопка загрузки нажата');

        const recordId = getRecordId();

        console.log('recordId:', recordId);

        if (!recordId) {
            status.textContent =
                'Ошибка: не удалось определить ID записи.';

            console.error(
                'Не удалось определить recordId из URL:',
                window.location.pathname
            );

            return;
        }


        if (!input.files || input.files.length === 0) {

            status.textContent =
                'Выберите хотя бы один видеофайл.';

            return;
        }


        /*
         * CSRF
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


        console.log('CSRF token найден:', !!csrfToken);
        console.log('CSRF header:', csrfHeader);


        button.disabled = true;

        progress.hidden = false;

        bar.value = 0;

        status.textContent =
            'Начинаем загрузку...';


        try {

            /*
             * Загружаем файлы один за другим.
             */
            for (
                let fileIndex = 0;
                fileIndex < input.files.length;
                fileIndex++
            ) {

                const file = input.files[fileIndex];

                console.log(
                    'Начинаем загрузку файла:',
                    file.name,
                    'размер:',
                    file.size
                );


                /*
                 * Уникальный ID одной загрузки.
                 *
                 * Все chunks одного файла имеют
                 * одинаковый uploadId.
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
                 * Загружаем chunks последовательно.
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
                        file.slice(start, end);


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


                    form.append(
                        'chunk',
                        chunk,
                        file.name + '.part'
                    );


                    /*
                     * HTTP headers.
                     */
                    const headers = {};


                    if (
                        csrfToken &&
                        csrfHeader
                    ) {

                        headers[csrfHeader] =
                            csrfToken;
                    }


                    /*
                     * URL контроллера.
                     *
                     * Например:
                     *
                     * /records/6ab25499bdb64082cbdc3bcd/videos/chunk
                     */
                    const url =
                        `/records/${recordId}/videos/chunk`;


                    console.log(
                        'Отправляем chunk:',
                        chunkNumber + 1,
                        '/',
                        totalChunks,
                        url
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


                    /*
                     * Процент загрузки текущего файла.
                     */
                    const percent =
                        Math.round(
                            (end / file.size) * 100
                        );


                    bar.value = percent;


                    status.textContent =
                        `Файл ${fileIndex + 1} из ` +
                        `${input.files.length}: ` +
                        `${file.name} — ${percent}%`;


                    /*
                     * Небольшая запись в консоль
                     * для контроля.
                     */
                    console.log(
                        `Файл ${file.name}: ` +
                        `chunk ${chunkNumber + 1}/${totalChunks}, ` +
    `${percent}%`
);
}


console.log(
    'Файл полностью загружен:',
    file.name
);
}


/*
 * Все файлы загружены.
 */
bar.value = 100;


status.textContent =
    'Все файлы успешно скопированы на видеодиск.';


console.log(
    'Загрузка всех файлов завершена'
);


/*
 * Обновляем страницу через 800 мс,
 * чтобы появился новый файл.
 */
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

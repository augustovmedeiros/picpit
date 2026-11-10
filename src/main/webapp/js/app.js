/*
 * Catálogo de Mídias - comportamento no navegador (primeira camada de validação).
 *
 * Melhoria progressiva: sem JavaScript, o formulário continua protegido pelos
 * atributos HTML5 (required, min, max, maxlength...) e, sempre, pela validação
 * no servidor (Validador) e pelas constraints do banco. Aqui apenas:
 *   - mostramos as mensagens em português ao lado de cada campo;
 *   - validamos o título "só com espaços", que o HTML5 aceitaria;
 *   - atualizamos o rótulo Autor/Criador/Diretor conforme o tipo;
 *   - contamos os caracteres da sinopse;
 *   - pedimos confirmação antes de excluir.
 *
 * Não há scripts inline: a Content-Security-Policy só permite este arquivo.
 */
(function () {
    'use strict';

    var CAMPOS_VALIDAVEIS = 'input:not([type=hidden]), select, textarea';

    function mensagemPara(campo) {
        var v = campo.validity;
        if (v.customError) {
            return campo.validationMessage;
        }
        if (v.valueMissing) {
            return campo.tagName === 'SELECT' ? 'Selecione uma opção.' : 'Este campo é obrigatório.';
        }
        if (v.badInput) {
            return 'Digite um número válido.';
        }
        if (v.tooShort) {
            return 'Use pelo menos ' + campo.minLength + ' caracteres (atualmente ' + campo.value.length + ').';
        }
        if (v.tooLong) {
            return 'Use no máximo ' + campo.maxLength + ' caracteres.';
        }
        if (v.rangeUnderflow || v.rangeOverflow) {
            return 'O valor deve estar entre ' + campo.min + ' e ' + campo.max + '.';
        }
        if (v.stepMismatch) {
            return campo.step === '1' ? 'Digite um número inteiro.' : 'Use no máximo uma casa decimal (ex.: 7.5).';
        }
        return campo.validationMessage || 'Valor inválido.';
    }

    /** Regras extras que o HTML5 não cobre. */
    function regrasExtras(campo) {
        campo.setCustomValidity('');
        if (campo.required && campo.type !== 'number' && campo.value.length > 0 && campo.value.trim().length === 0) {
            campo.setCustomValidity('Este campo não pode conter apenas espaços.');
        } else if (campo.minLength > 0 && campo.value.length > 0 && campo.value.trim().length < campo.minLength) {
            campo.setCustomValidity('Use pelo menos ' + campo.minLength + ' caracteres, sem contar espaços.');
        }
    }

    function exibirErro(campo, mensagem) {
        var caixa = campo.closest('.campo');
        var saida = document.getElementById('erro-' + campo.id);
        if (saida) {
            saida.textContent = mensagem || '';
        }
        if (caixa) {
            caixa.classList.toggle('campo--invalido', !!mensagem);
        }
        if (mensagem) {
            campo.setAttribute('aria-invalid', 'true');
        } else {
            campo.removeAttribute('aria-invalid');
        }
    }

    function validarCampo(campo) {
        regrasExtras(campo);
        var valido = campo.checkValidity();
        exibirErro(campo, valido ? '' : mensagemPara(campo));
        return valido;
    }

    function configurarFormulario(form) {
        // Assumimos a exibição das mensagens (sem os balões nativos do navegador).
        form.noValidate = true;
        var campos = Array.prototype.slice.call(form.querySelectorAll(CAMPOS_VALIDAVEIS));

        campos.forEach(function (campo) {
            campo.addEventListener('blur', function () {
                if (campo.value !== '' || campo.dataset.tocado) {
                    validarCampo(campo);
                }
                campo.dataset.tocado = '1';
            });
            campo.addEventListener('input', function () {
                // Enquanto o usuário corrige, a mensagem some assim que o valor fica válido.
                if (campo.getAttribute('aria-invalid') === 'true') {
                    validarCampo(campo);
                }
            });
            campo.addEventListener('change', function () {
                if (campo.tagName === 'SELECT') {
                    validarCampo(campo);
                }
            });
        });

        form.addEventListener('submit', function (evento) {
            var primeiroInvalido = null;
            campos.forEach(function (campo) {
                if (!validarCampo(campo) && !primeiroInvalido) {
                    primeiroInvalido = campo;
                }
            });
            if (primeiroInvalido) {
                evento.preventDefault();
                primeiroInvalido.focus();
                return;
            }
            var botao = form.querySelector('button[type=submit]');
            if (botao) {
                botao.disabled = true;
                botao.textContent = 'Salvando…';
            }
        });
    }

    function configurarRotuloCriador() {
        var tipo = document.getElementById('tipo');
        var rotulo = document.getElementById('rotulo-criador');
        if (!tipo || !rotulo) {
            return;
        }
        var padrao = rotulo.textContent;
        tipo.addEventListener('change', function () {
            var opcao = tipo.options[tipo.selectedIndex];
            rotulo.textContent = (opcao && opcao.dataset.rotuloCriador) || padrao;
        });
    }

    function configurarContadores() {
        document.querySelectorAll('[data-contador]').forEach(function (campo) {
            var saida = document.getElementById(campo.dataset.contador);
            if (!saida) {
                return;
            }
            var atualizar = function () {
                var restante = campo.maxLength - campo.value.length;
                saida.textContent = campo.value.length + '/' + campo.maxLength + ' caracteres';
                saida.classList.toggle('campo__dica--alerta', restante <= 50);
            };
            campo.addEventListener('input', atualizar);
            atualizar();
        });
    }

    function configurarConfirmacoes() {
        document.querySelectorAll('form[data-confirmar]').forEach(function (form) {
            form.addEventListener('submit', function (evento) {
                if (!window.confirm(form.dataset.confirmar)) {
                    evento.preventDefault();
                }
            });
        });
    }

    function configurarAlertas() {
        document.querySelectorAll('[data-fechavel]').forEach(function (alerta) {
            var botao = alerta.querySelector('.alerta__fechar');
            if (botao) {
                botao.hidden = false;
                botao.addEventListener('click', function () {
                    alerta.remove();
                });
            }
        });
        var resumo = document.getElementById('resumo-erros');
        if (resumo) {
            resumo.focus();
        }
    }

    function configurarVoltar() {
        document.querySelectorAll('[data-voltar]').forEach(function (link) {
            if (window.history.length > 1) {
                link.hidden = false;
                link.addEventListener('click', function (evento) {
                    evento.preventDefault();
                    window.history.back();
                });
            }
        });
    }

    document.addEventListener('DOMContentLoaded', function () {
        document.querySelectorAll('form[data-validar]').forEach(configurarFormulario);
        configurarRotuloCriador();
        configurarContadores();
        configurarConfirmacoes();
        configurarAlertas();
        configurarVoltar();
    });
})();

